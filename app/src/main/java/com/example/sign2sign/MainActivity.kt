package com.example.sign2sign

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.sign2sign.databinding.ActivityMainBinding
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var tflite: Interpreter? = null
    private var labels: List<String> = emptyList()

    private lateinit var languageManager: LanguageManager
    private lateinit var grammarProcessor: GrammarProcessor
    private lateinit var signVisualizer: SignVisualizer

    private val liveWordsBn: ArrayDeque<String> = ArrayDeque()
    private var sentenceOnScreen: Boolean = false
    private val postSentenceBuffer: MutableList<String> = mutableListOf()
    private var lastSentenceTokens: List<String> = emptyList()
    private var lastSentenceBn: String = ""

    private data class ModelConfig(
        val modelPath: String,
        val labelsPath: String,
        val inputWidth: Int,
        val inputHeight: Int,
        val modelAvailable: Boolean
    )

    private val MODEL_CONFIGS = mapOf(
        "bdsl" to ModelConfig("best.tflite", "labels.txt", 416, 416, true),
        "asl"  to ModelConfig("asl_model.tflite", "asl_labels.txt", 416, 416, false)
    )
    private var currentModelConfig = MODEL_CONFIGS["bdsl"]!!

    private val CONFIDENCE_THRESHOLD = 0.70f
    private val IOU_THRESHOLD = 0.50f

    private val mySignOptions = listOf("ASL (American)", "BDSL (Bangla Sign)", "Spanish")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        languageManager = LanguageManager()
        grammarProcessor = GrammarProcessor()
        signVisualizer = SignVisualizer(this)

        setupLanguageSelectors()
        setupUI()

        if (allPermissionsGranted()) {
            if (setupTFLiteInterpreter()) startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupUI() {
        binding.instructionText.text = "Select source and target languages, then point camera at sign language user"
        styleVisualToggle()
        tintClearButton()
        binding.clearButton.setOnClickListener {
            grammarProcessor.reset()
            liveWordsBn.clear()
            postSentenceBuffer.clear()
            lastSentenceTokens = emptyList()
            lastSentenceBn = ""
            sentenceOnScreen = false
            binding.translatedSentenceText.text = ""
            binding.gestureDescriptionText.text = ""
            binding.suttonSignText.text = ""
            binding.suttonSignContainer.visibility = View.GONE
            binding.overlay.clear()
            updateUI(forceReRender = true)
        }
        binding.visualOutputToggle.setOnCheckedChangeListener { _, _ ->
            updateUI(forceReRender = true)
        }
    }

    private fun tintClearButton() {
        val bg = ContextCompat.getColor(this, R.color.shrine_primary)
        val fg = ContextCompat.getColor(this, R.color.shrine_on_primary)
        binding.clearButton.backgroundTintList = ColorStateList.valueOf(bg)
        binding.clearButton.setTextColor(fg)
    }

    private fun setupLanguageSelectors() {
        val inAdapter = makeDarkSpinnerAdapter(languageManager.getAvailableInputLanguages())
        binding.inputLanguageSpinner.adapter = inAdapter
        binding.inputLanguageSpinner.applyDarkStyle()

        val outAdapter = makeDarkSpinnerAdapter(languageManager.getAvailableOutputLanguages())
        binding.outputLanguageSpinner.adapter = outAdapter
        binding.outputLanguageSpinner.applyDarkStyle()

        val myAdapter = makeDarkSpinnerAdapter(mySignOptions)
        binding.myLanguageSpinner.adapter = myAdapter
        binding.myLanguageSpinner.applyDarkStyle()

        binding.inputLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = languageManager.getAvailableInputLanguages()[pos]
                languageManager.setInputLanguage(selected)
                switchSignLanguageModel(getLanguageKey(selected))
                updateUI(forceReRender = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.outputLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                languageManager.setOutputLanguage(languageManager.getAvailableOutputLanguages()[pos])
                updateUI(forceReRender = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.myLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                updateUI(forceReRender = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun makeDarkSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        val ctx = this
        val darkBg = ContextCompat.getColor(ctx, R.color.shrine_primary)
        val darkText = ContextCompat.getColor(ctx, R.color.shrine_on_primary)
        val dropBg = ContextCompat.getColor(ctx, R.color.shrine_primary_container)
        val dropText = ContextCompat.getColor(ctx, R.color.shrine_on_primary_container)

        return object : ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setBackgroundColor(darkBg)
                v.setTextColor(darkText)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setBackgroundColor(dropBg)
                v.setTextColor(dropText)
                return v
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun Spinner.applyDarkStyle() {
        background?.setTint(ContextCompat.getColor(context, R.color.shrine_primary))
        (this as? androidx.appcompat.widget.AppCompatSpinner)
            ?.setPopupBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, R.color.shrine_primary_container)))
    }

    private fun styleVisualToggle() {
        val track = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(
                ContextCompat.getColor(this, R.color.shrine_primary_container),
                ContextCompat.getColor(this, R.color.shrine_primary_container)
            )
        )
        val thumb = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(
                ContextCompat.getColor(this, R.color.shrine_primary),
                ContextCompat.getColor(this, R.color.shrine_primary)
            )
        )
        binding.visualOutputToggle.trackTintList = track
        binding.visualOutputToggle.thumbTintList = thumb
        binding.visualOutputToggle.setTextColor(ContextCompat.getColor(this, R.color.shrine_on_surface))
    }

    private fun isASLSelectedForVisuals(): Boolean {
        val sel = binding.myLanguageSpinner.selectedItem?.toString() ?: ""
        return sel.contains("ASL", true)
    }

    private fun getLanguageKey(name: String) = when {
        name.contains("BDSL", true) || name.contains("Bengali", true) -> "bdsl"
        name.contains("ASL", true) -> "asl"
        else -> "bdsl"
    }

    private fun switchSignLanguageModel(language: String) {
        currentModelConfig = MODEL_CONFIGS[language] ?: currentModelConfig
        tflite?.close()
        if (currentModelConfig.modelAvailable) {
            if (setupTFLiteInterpreter()) {
                showToast("Using $language sign model")
            }
        } else {
            showToast("Model for $language not available")
            tflite = null
            labels = emptyList()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupTFLiteInterpreter(): Boolean {
        return try {
            val hasModel = try {
                assets.open(currentModelConfig.modelPath).close(); true
            } catch (_: Exception) { false }
            if (!hasModel) {
                showToast("Missing ${currentModelConfig.modelPath} in assets/")
                return false
            }
            val buffer = loadModelFile(assets, currentModelConfig.modelPath)
            tflite = Interpreter(buffer, Interpreter.Options().apply {
                setNumThreads(4)
                @Suppress("DEPRECATION")
                setUseXNNPACK(true)
                setUseNNAPI(false)
            })
            labels = try { FileUtil.loadLabels(this, currentModelConfig.labelsPath) } catch (_: Exception) { emptyList() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "TFLite setup error: ${e.message}", e)
            showToast("Model load failed")
            false
        }
    }

    private fun loadModelFile(am: AssetManager, path: String): ByteBuffer {
        val fd = am.openFd(path)
        val input = FileInputStream(fd.fileDescriptor)
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::detectObjects) }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e(TAG, "bind use cases failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun detectObjects(imageProxy: ImageProxy) {
        try {
            var detectedSign: String? = null
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null && tflite != null && currentModelConfig.modelAvailable) {
                val rotated = rotateBitmap(bitmap, 90)
                val processor = ImageProcessor.Builder()
                    .add(ResizeOp(currentModelConfig.inputHeight, currentModelConfig.inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                    .add(NormalizeOp(0.0f, 255.0f))
                    .build()
                var tensorImg = TensorImage.fromBitmap(rotated)
                tensorImg = processor.process(tensorImg)
                val outTensor = tflite!!.getOutputTensor(0)
                val shape = outTensor.shape()
                val numPredictions = shape[1]
                val numElements = shape[2]
                val outBuf = ByteBuffer.allocateDirect(1 * numPredictions * numElements * 4).apply { order(ByteOrder.nativeOrder()) }
                tflite!!.run(tensorImg.buffer, outBuf)
                outBuf.rewind()
                val fbuf = outBuf.asFloatBuffer()
                val results = processYoloOutput(fbuf, numPredictions, numElements)
                if (results.isNotEmpty()) detectedSign = results.maxByOrNull { it.score }?.label
                runOnUiThread { binding.overlay.setResults(results, imageProxy.height, imageProxy.width) }
            } else {
                runOnUiThread { binding.overlay.clear() }
            }
            grammarProcessor.processDetection(detectedSign)
            if (grammarProcessor.tokenChanged || grammarProcessor.sentenceChanged) {
                runOnUiThread { updateUI(forceReRender = false) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "detectObjects error", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun processYoloOutput(buffer: FloatBuffer, numPredictions: Int, numElements: Int): List<DetectionResult> {
        val out = mutableListOf<DetectionResult>()
        val iw = currentModelConfig.inputWidth.toFloat()
        val ih = currentModelConfig.inputHeight.toFloat()
        if (numElements == 6) {
            var sampleMax = 0f
            val sample = min(10, numPredictions)
            for (i in 0 until sample) {
                val b = i * 6
                sampleMax = maxOf(
                    sampleMax,
                    kotlin.math.abs(buffer.get(b + 0)),
                    kotlin.math.abs(buffer.get(b + 1)),
                    kotlin.math.abs(buffer.get(b + 2)),
                    kotlin.math.abs(buffer.get(b + 3))
                )
            }
            val coordsAreNormalized = sampleMax <= 1.5f
            for (i in 0 until numPredictions) {
                val base = i * 6
                var x1 = buffer.get(base + 0)
                var y1 = buffer.get(base + 1)
                var x2 = buffer.get(base + 2)
                var y2 = buffer.get(base + 3)
                val conf = buffer.get(base + 4)
                val clsIdx = buffer.get(base + 5).toInt()
                if (conf.isNaN() || conf < CONFIDENCE_THRESHOLD) continue
                if (clsIdx !in labels.indices) continue
                if (!coordsAreNormalized) {
                    x1 /= iw; y1 /= ih; x2 /= iw; y2 /= ih
                }
                x1 = x1.coerceIn(0f, 1f); y1 = y1.coerceIn(0f, 1f)
                x2 = x2.coerceIn(0f, 1f); y2 = y2.coerceIn(0f, 1f)
                val rect = RectF(min(x1, x2) * iw, min(y1, y2) * ih, max(x1, x2) * iw, max(y1, y2) * ih)
                val minArea = 0.01f * iw * ih
                if ((rect.width() * rect.height()) < minArea) continue
                out += DetectionResult(rect, labels[clsIdx], conf)
            }
            return applyNMS(out, IOU_THRESHOLD)
        }
        return out
    }

    private fun applyNMS(dets: List<DetectionResult>, iouThr: Float): List<DetectionResult> {
        if (dets.isEmpty()) return emptyList()
        val sorted = dets.sortedByDescending { it.score }.toMutableList()
        val keep = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            keep.add(best)
            val it = sorted.iterator()
            while (it.hasNext()) {
                val d = it.next()
                if (iou(best.boundingBox, d.boundingBox) > iouThr) it.remove()
            }
        }
        return keep
    }

    private fun iou(a: RectF, b: RectF): Float {
        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)
        val x2 = kotlin.math.min(a.right, b.right)
        val y2 = kotlin.math.min(a.bottom, b.bottom)
        val inter = kotlin.math.max(0f, x2 - x1) * kotlin.math.max(0f, y2 - y1)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        return if (areaA + areaB - inter <= 0f) 0f else inter / (areaA + areaB - inter)
    }

    private fun translateTokenForOutput(tokenBn: String): String {
        val outLang = languageManager.getOutputLanguage()
        if (outLang.contains("Bengali", true)) return tokenBn
        val uni = languageManager.signToUniversal(tokenBn) ?: languageManager.signToUniversal(tokenBn.lowercase())
        val out = uni?.let { languageManager.universalToOutput(it) }
        if (!out.isNullOrBlank()) return out
        return (TranslationUtils.BANGLA_WORD_TO_ENGLISH[tokenBn] ?: tokenBn)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun updateUI(forceReRender: Boolean) {
        try {
            val sw = ResourcesCompat.getFont(this, R.font.noto_sans_signwriting)
            if (sw != null && binding.suttonSignText.typeface != sw) {
                binding.suttonSignText.typeface = sw
            }
        } catch (_: Exception) {}

        val visualToggleOn = binding.visualOutputToggle.isChecked
        val allowSutton = visualToggleOn && isASLSelectedForVisuals()
        signVisualizer.setAllowSuttonGlyphs(allowSutton)

        if (grammarProcessor.sentenceChanged) {
            val bnSentence = grammarProcessor.lastFormedSentence.trim()
            val bnNorm = bnSentence.replace("\\s+".toRegex(), " ").trim()
                .let { if (it.endsWith("।") || it.endsWith("?") || it.endsWith(".")) it else "$it।" }
            val newTokens = bnNorm.trim('।', '?', '.').split(" ").filter { it.isNotBlank() }
            if (sentenceOnScreen && newTokens.size == 1 &&
                lastSentenceTokens.isNotEmpty() &&
                newTokens[0] == lastSentenceTokens.last()
            ) {
                // Ignore duplicate single-word sentence (hold-last-word scenario)
                grammarProcessor.clearLastSentence()
                return
            }
            lastSentenceBn = bnNorm
            renderSentence(bnNorm, allowSutton, visualToggleOn)
            sentenceOnScreen = true
            postSentenceBuffer.clear()
            liveWordsBn.clear()
            lastSentenceTokens = newTokens
            grammarProcessor.clearLastSentence()
            return
        }

        if (forceReRender) {
            if (sentenceOnScreen && lastSentenceBn.isNotBlank()) {
                renderSentence(lastSentenceBn, allowSutton, visualToggleOn)
                return
            } else {
                renderWords(allowSutton, visualToggleOn)
                return
            }
        }

        if (grammarProcessor.tokenChanged) {
            val tokenBn = grammarProcessor.getLastStableToken()
            grammarProcessor.clearTokenChanged()
            if (!tokenBn.isNullOrBlank()) {
                if (sentenceOnScreen) {
                    if (lastSentenceTokens.isNotEmpty() && tokenBn == lastSentenceTokens.last()) {
                        // Hold-last-word rule: user is holding the last sign of the sentence
                        // Keep the sentence displayed (refresh timer) and do not add this token
                        postSentenceBuffer.clear()
                        grammarProcessor.reset()
                    } else {
                        // Add token after a sentence; remove sentence if 3 new words collected
                        postSentenceBuffer.add(tokenBn)
                        if (postSentenceBuffer.size >= 3) {
                            // Three random word rule: clear old sentence and show these words
                            binding.translatedSentenceText.text = ""
                            sentenceOnScreen = false
                            liveWordsBn.clear()
                            postSentenceBuffer.forEach { liveWordsBn.addLast(it) }
                            postSentenceBuffer.clear()
                            lastSentenceTokens = emptyList()
                            lastSentenceBn = ""
                            grammarProcessor.reset()
                        }
                    }
                } else {
                    liveWordsBn.addLast(tokenBn)
                    while (liveWordsBn.size > 8) liveWordsBn.removeFirst()
                }
            }
        }

        if (!sentenceOnScreen) renderWords(allowSutton, visualToggleOn)
    }

    private fun renderSentence(bnSentence: String, allowSutton: Boolean, visualToggleOn: Boolean) {
        val outLang = languageManager.getOutputLanguage()
        val en = TranslationUtils.BANGLA_SENTENCE_TO_ENGLISH[bnSentence]
            ?: TranslationUtils.BANGLA_SENTENCE_TO_ENGLISH[bnSentence.trim()]
        val outText = when {
            outLang.contains("English", true) -> en ?: bnSentence.trim('।', '?', '.')
            outLang.contains("Bengali", true) -> bnSentence
            outLang.contains("Spanish", true) -> {
                val normalizedEn = en ?: bnSentence.trim('।', '?', '.')
                TranslationUtils.ENGLISH_SENTENCE_TO_SPANISH[normalizedEn] ?: normalizedEn
            }
            else -> en ?: bnSentence
        }
        val finalText = if (outLang.contains("Bengali", true)) {
            if (outText.endsWith("।") || outText.endsWith("?")) outText else "$outText।"
        } else {
            val t = outText.trim()
            if (bnSentence.endsWith("?")) {
                if (t.endsWith("?")) t else "$t?"
            } else {
                if (t.endsWith(".") || t.endsWith("?")) t else "$t."
            }
        }
        binding.translatedSentenceText.text = finalText
        if (visualToggleOn) {
            val visual = signVisualizer.forSentence(bnSentence)
            binding.suttonSignText.text = visual
            binding.suttonSignContainer.visibility = View.VISIBLE
        } else {
            binding.suttonSignText.text = ""
            binding.suttonSignContainer.visibility = View.GONE
        }
        binding.gestureDescriptionText.text = ""
    }

    private fun renderWords(allowSutton: Boolean, visualToggleOn: Boolean) {
        val wordTexts = liveWordsBn.map { translateTokenForOutput(it) }
        binding.gestureDescriptionText.text = wordTexts.joinToString(" ").trim()
        if (visualToggleOn) {
            val visuals = liveWordsBn.map { signVisualizer.forWord(it) }
            binding.suttonSignText.text = visuals.joinToString(" ").trim()
            binding.suttonSignContainer.visibility = View.VISIBLE
        } else {
            binding.suttonSignText.text = ""
            binding.suttonSignContainer.visibility = View.GONE
        }
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                if (setupTFLiteInterpreter()) startCamera()
            } else {
                showToast("Camera permission required")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tflite?.close()
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        planes[0].buffer.get(nv21, 0, ySize)
        val pixelStride = planes[2].pixelStride
        if (pixelStride == 2) {
            var pos = ySize
            val uvSize = min(planes[1].buffer.remaining(), planes[2].buffer.remaining())
            for (i in 0 until uvSize) {
                if (pos + 1 < nv21.size) {
                    nv21[pos] = planes[2].buffer.get(i)
                    nv21[pos + 1] = planes[1].buffer.get(i)
                    pos += 2
                }
            }
        } else {
            val uBuf = ByteArray(uSize)
            val vBuf = ByteArray(vSize)
            planes[1].buffer.get(uBuf)
            planes[2].buffer.get(vBuf)
            System.arraycopy(vBuf, 0, nv21, ySize, vSize)
        }
        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, this.width, this.height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    companion object {
        private const val TAG = "Sign2SignApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
