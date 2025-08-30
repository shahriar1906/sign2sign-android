package com.example.sign2sign

class LanguageManager {
    private var inputLanguage = "BDSL (Bangla Sign)"
    private var outputLanguage = "Bengali"
    private val availableInputLanguages = listOf("BDSL (Bangla Sign)", "ASL (American)")
    private val availableOutputLanguages = listOf("Bengali", "English", "Spanish")

    private val universalSignMappings: Map<String, String> = mapOf(
        "সাহায্য" to "HELP","অপেক্ষা" to "WAIT","অপেক্ষা করো" to "WAIT","চুপ" to "QUIET","চুপ করো" to "QUIET","দুঃখিত" to "SORRY",
        "আমি" to "I","তুমি" to "YOU","ভালো" to "GOOD","খারাপ" to "BAD","ক্ষুধার্ত" to "HUNGRY","ক্লান্ত" to "TIRED",
        "কে" to "WHO","কখন" to "WHEN","মাথাব্যথা" to "HEADACHE","কাজ" to "WORK","আমার" to "MY","তোমার" to "YOUR",
        "এখন" to "NOW","আজ" to "TODAY","আজে" to "TODAY","হ্যাঁ" to "YES","না" to "NO","ধন্যবাদ" to "THANK YOU",
        "help" to "HELP","wait" to "WAIT","quiet" to "QUIET","sorry" to "SORRY","who" to "WHO","when" to "WHEN",
        "good" to "GOOD","bad" to "BAD","hungry" to "HUNGRY","tired" to "TIRED","headache" to "HEADACHE",
        "work" to "WORK","my" to "MY","your" to "YOUR","now" to "NOW","today" to "TODAY","i" to "I","you" to "YOU",
        "yes" to "YES","no" to "NO","thank you" to "THANK YOU"
    )

    private val universalToOutputLanguage: Map<String, Map<String, String>> = mapOf(
        "English" to mapOf(
            "HELP" to "Help","WAIT" to "Wait","QUIET" to "Be quiet","SORRY" to "Sorry","I" to "I","YOU" to "You",
            "GOOD" to "Good","BAD" to "Bad","HUNGRY" to "Hungry","TIRED" to "Tired","WHO" to "Who","WHEN" to "When",
            "HEADACHE" to "Headache","WORK" to "Work","MY" to "My","YOUR" to "Your","NOW" to "Now","TODAY" to "Today",
            "YES" to "Yes","NO" to "No","THANK YOU" to "Thank you"
        ),
        "Bengali" to mapOf(
            "HELP" to "সাহায্য","WAIT" to "অপেক্ষা","QUIET" to "চুপ","SORRY" to "দুঃখিত","I" to "আমি","YOU" to "তুমি",
            "GOOD" to "ভালো","BAD" to "খারাপ","HUNGRY" to "ক্ষুধার্ত","TIRED" to "ক্লান্ত","WHO" to "কে","WHEN" to "কখন",
            "HEADACHE" to "মাথাব্যথা","WORK" to "কাজ","MY" to "আমার","YOUR" to "তোমার","NOW" to "এখন","TODAY" to "আজ",
            "YES" to "হ্যাঁ","NO" to "না","THANK YOU" to "ধন্যবাদ"
        ),
        "Spanish" to mapOf(
            "HELP" to "Ayuda","WAIT" to "Espera","QUIET" to "Silencio","SORRY" to "Lo siento","I" to "Yo","YOU" to "Tú",
            "GOOD" to "Bueno","BAD" to "Malo","HUNGRY" to "Hambriento","TIRED" to "Cansado","WHO" to "Quién","WHEN" to "Cuándo",
            "HEADACHE" to "Dolor de cabeza","WORK" to "Trabajo","MY" to "Mi","YOUR" to "Tu","NOW" to "Ahora","TODAY" to "Hoy",
            "YES" to "Sí","NO" to "No","THANK YOU" to "Gracias"
        )
    )

    fun getAvailableInputLanguages(): List<String> = availableInputLanguages
    fun getAvailableOutputLanguages(): List<String> = availableOutputLanguages
    fun setInputLanguage(lang: String) { inputLanguage = lang }
    fun setOutputLanguage(lang: String) { outputLanguage = lang }
    fun getOutputLanguage(): String = outputLanguage
    fun signToUniversal(token: String): String? {
        val key = token.trim()
        return universalSignMappings[key] ?: universalSignMappings[key.lowercase()]
    }
    fun universalToOutput(u: String): String? = universalToOutputLanguage[outputLanguage]?.get(u)
}
