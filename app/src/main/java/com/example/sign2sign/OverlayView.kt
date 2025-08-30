package com.example.sign2sign

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.R as MaterialR
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<DetectionResult>? = null
    private val boxPaint = Paint()
    private val textBackgroundPaint = Paint()
    private val textPaint = Paint()

    private var scaleFactorX: Float = 1f
    private var scaleFactorY: Float = 1f
    private var modelInputWidth: Int = 1
    private var modelInputHeight: Int = 1

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        val typedValue = android.util.TypedValue()
        context!!.theme.resolveAttribute(MaterialR.attr.colorPrimary, typedValue, true)
        boxPaint.color = typedValue.data
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results?.forEach { result ->
            val boundingBox = result.boundingBox

            val left = boundingBox.left * scaleFactorX
            val top = boundingBox.top * scaleFactorY
            val right = boundingBox.right * scaleFactorX
            val bottom = boundingBox.bottom * scaleFactorY

            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            val drawableText = "${result.label} ${String.format("%.2f", result.score)}"
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(drawableText, 0, drawableText.length, textBounds)
            val textWidth = textBounds.width()
            val textHeight = textBounds.height()

            canvas.drawRect(
                left,
                top - textHeight - 4, // Add padding
                left + textWidth + 8, // Add padding
                top,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left + 4, top - 4, textPaint)
        }
    }

    fun setResults(
        detectionResults: List<DetectionResult>,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = detectionResults

        // --- FINAL FIX: These values must match the model's actual input size ---
        modelInputWidth = 416
        modelInputHeight = 416

        scaleFactorX = width.toFloat() / modelInputWidth
        scaleFactorY = height.toFloat() / modelInputHeight

        invalidate()
    }

    fun clear() {
        results = null
        invalidate()
    }
}