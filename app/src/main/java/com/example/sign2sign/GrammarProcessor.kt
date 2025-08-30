package com.example.sign2sign

class GrammarProcessor {

    private val TOKEN_MIN_FRAMES = 2
    private val TOKEN_COOLDOWN_FRAMES = 5
    private val SENTENCE_GAP_FRAMES = 40
    private val INLINE_MAX_WINDOW = 6

    private fun normalize(raw: String?): String? = when (raw?.trim()) {
        null -> null
        "আজে" -> "আজ"
        else -> raw.trim()
    }

    private val grammarRules: Map<List<String>, String> = mapOf(
        listOf("অপেক্ষা করো") to "অপেক্ষা করো।",
        listOf("চুপ করো") to "চুপ করো।",
        listOf("সাহায্য") to "সাহায্য করুন।",
        listOf("দুঃখিত") to "দুঃখিত।",
        listOf("কে") to "কে?",
        listOf("কখন") to "কখন?",
        listOf("ভালো") to "ভালো।",
        listOf("খারাপ") to "খারাপ।",
        listOf("আমি", "ভালো") to "আমি ভালো আছি।",
        listOf("আমি", "খারাপ") to "আমি ভালো নেই।",
        listOf("আমি", "ক্ষুধার্ত") to "আমি ক্ষুধার্ত।",
        listOf("আমি", "ক্লান্ত") to "আমি ক্লান্ত।",
        listOf("তুমি", "ভালো") to "তুমি ভালো আছো?",
        listOf("তুমি", "খারাপ") to "তুমি ভালো নও?",
        listOf("তুমি", "ক্ষুধার্ত") to "তুমি ক্ষুধার্ত?",
        listOf("তুমি", "ক্লান্ত") to "তুমি ক্লান্ত?",
        listOf("আমার", "মাথাব্যথা") to "আমার মাথাব্যথা হচ্ছে।",
        listOf("তোমার", "মাথাব্যথা") to "তোমার মাথাব্যথা আছে?",
        listOf("এখন", "কাজ") to "এখন কাজ করো।",
        listOf("আজ", "কাজ") to "আজ কাজ আছে।",
        listOf("আমার", "কাজ") to "আমার কাজ আছে।",
        listOf("তোমার", "কাজ") to "তোমার কাজ আছে।"
    )

    private val currentTokens = mutableListOf<String>()

    var lastFormedSentence: String = ""
        private set
    var sentenceChanged: Boolean = false
        private set

    private var lastStableToken: String? = null
    var tokenChanged: Boolean = false
        private set

    private var lastFrameLabel: String? = null
    private var sameLabelFrameCount = 0
    private var cooldownFrameCount = 0
    private var framesSinceLastToken = 0

    fun getCurrentTokensString(): String = currentTokens.joinToString(" ")
    fun getLastStableToken(): String? = lastStableToken
    fun clearTokenChanged() { tokenChanged = false }
    fun clearLastSentence() { lastFormedSentence = ""; sentenceChanged = false }

    fun reset() {
        currentTokens.clear()
        lastStableToken = null
        lastFormedSentence = ""
        sentenceChanged = false
        tokenChanged = false
        lastFrameLabel = null
        sameLabelFrameCount = 0
        cooldownFrameCount = 0
        framesSinceLastToken = 0
    }

    fun processDetection(detectedLabelRaw: String?) {
        sentenceChanged = false
        if (cooldownFrameCount > 0) cooldownFrameCount--

        val detectedLabel = normalize(detectedLabelRaw)
        if (detectedLabel != null) {
            if (detectedLabel == lastFrameLabel) sameLabelFrameCount++ else {
                lastFrameLabel = detectedLabel
                sameLabelFrameCount = 1
            }
            if (sameLabelFrameCount >= TOKEN_MIN_FRAMES && cooldownFrameCount == 0) {
                if (currentTokens.isEmpty() || currentTokens.last() != detectedLabel) {
                    currentTokens.add(detectedLabel)
                    lastStableToken = detectedLabel
                    tokenChanged = true
                    framesSinceLastToken = 0
                    cooldownFrameCount = TOKEN_COOLDOWN_FRAMES
                    checkInlineSentence()?.let { sentence ->
                        lastFormedSentence = sentence
                        sentenceChanged = true
                        lastFrameLabel = null
                        sameLabelFrameCount = 0
                        cooldownFrameCount = 0
                        framesSinceLastToken = 0
                        return
                    }
                }
            } else {
                framesSinceLastToken++
            }
        } else {
            framesSinceLastToken++
        }

        if (currentTokens.isNotEmpty() && framesSinceLastToken >= SENTENCE_GAP_FRAMES) {
            val tokensCopy = currentTokens.toList()
            currentTokens.clear()
            framesSinceLastToken = 0
            lastFrameLabel = null
            sameLabelFrameCount = 0
            cooldownFrameCount = 0
            val finalSentence = buildSentence(tokensCopy)
            if (finalSentence.isNotBlank()) {
                lastFormedSentence = finalSentence
                sentenceChanged = true
            }
        }
    }

    private fun checkInlineSentence(): String? {
        val n = currentTokens.size
        val maxWin = minOf(INLINE_MAX_WINDOW, n)
        for (win in maxWin downTo 1) {
            for (start in 0..(n - win)) {
                val slice = currentTokens.subList(start, start + win).toList()
                val merged = mergeCommonPairs(slice)
                grammarRules[merged]?.let {
                    repeat(win) { currentTokens.removeAt(start) }
                    return it
                }
            }
        }
        return null
    }

    private fun buildSentence(tokensRaw: List<String>): String {
        if (tokensRaw.isEmpty()) return ""
        val tokens = tokensRaw.mapNotNull { normalize(it) }
        grammarRules[tokens]?.let { return it }
        val merged = mergeCommonPairs(tokens)
        grammarRules[merged]?.let { return it }
        val raw = merged.joinToString(" ").trim()
        if (raw.isBlank()) return ""
        return if (raw.endsWith("।") || raw.endsWith("?")) raw else "$raw।"
    }

    private fun mergeCommonPairs(tokens: List<String>): List<String> {
        if (tokens.size < 2) return tokens
        val out = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            if (i + 1 < tokens.size) {
                val a = tokens[i]; val b = tokens[i + 1]
                if (a == "অপেক্ষা" && b == "করো") { out += "অপেক্ষা করো"; i += 2; continue }
                if (a == "চুপ" && b == "করো") { out += "চুপ করো"; i += 2; continue }
            }
            out += tokens[i]; i++
        }
        return out
    }
}
