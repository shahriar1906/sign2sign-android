package com.example.sign2sign

import android.content.Context

class SignVisualizer(private val context: Context) {
    private var allowSuttonGlyphs = true
    fun setAllowSuttonGlyphs(allow: Boolean) { allowSuttonGlyphs = allow }

    private fun bnToEnWord(bn: String): String =
        TranslationUtils.BANGLA_WORD_TO_ENGLISH[bn.trim()] ?: bn.trim()

    private fun bnToEnSentence(bnSentence: String): String {
        val normalized = bnSentence
            .replace("\\s+".toRegex(), " ")
            .trim()
            .let { if (it.endsWith("।") || it.endsWith("?") || it.endsWith(".")) it else "$it।" }
        return TranslationUtils.BANGLA_SENTENCE_TO_ENGLISH[normalized]
            ?: TranslationUtils.BANGLA_SENTENCE_TO_ENGLISH[bnSentence.trim()]
            ?: bnSentence
    }

    fun forWord(wordOrToken: String): String {
        val raw = wordOrToken.trim()
        if (raw.isEmpty()) return ""
        val english = bnToEnWord(raw).trim().replaceFirstChar { it.uppercase() }
        if (allowSuttonGlyphs) {
            TranslationUtils.ENGLISH_WORD_TO_SIGNWRITING[english]?.let { if (it.isNotBlank()) return it }
        }
        TranslationUtils.ENGLISH_WORD_TO_EMOJI[english.lowercase()]?.let { return it }
        return "✋"
    }

    fun forSentence(sentenceText: String): String {
        val s = sentenceText.trim()
        if (s.isEmpty()) return ""
        val isBangla = s.any { it.code in 0x0980..0x09FF }
        val english = if (isBangla) bnToEnSentence(s) else s
        val normalized = english.trim()
        if (allowSuttonGlyphs) {
            TranslationUtils.ENGLISH_SENTENCE_TO_SIGNWRITING[normalized]?.let { if (it.isNotBlank()) return it }
        }
        TranslationUtils.ENGLISH_SENTENCE_TO_EMOJI[normalized]?.let { return it }
        val words = normalized.trim('।', '?', '.', '!').split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.joinToString(" ") { w -> forWord(w) }
    }

    fun getSignRepresentation(sign: String): String = forWord(sign)
}
