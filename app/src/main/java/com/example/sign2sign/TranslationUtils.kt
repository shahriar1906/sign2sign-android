package com.example.sign2sign

object TranslationUtils {
    val BANGLA_SENTENCE_TO_ENGLISH = mapOf(
        "সাহায্য করুন।" to "Help please.","অপেক্ষা করো।" to "Please wait.","দুঃখিত।" to "I am sorry.",
        "চুপ করো।" to "Be quiet.","কে?" to "Who?","কখন?" to "When?","ভালো।" to "It's good.","খারাপ।" to "It's bad.",
        "আমি ভালো আছি।" to "I am fine.","আমি ভালো নেই।" to "I am not well.","আমি ক্ষুধার্ত।" to "I am hungry.",
        "আমি ক্লান্ত।" to "I am tired.","তুমি ভালো আছো?" to "Are you okay?","তুমি ভালো নও?" to "Are you not well?",
        "তুমি ক্ষুধার্ত?" to "Are you hungry?","তুমি ক্লান্ত?" to "Are you tired?","আমার মাথাব্যথা হচ্ছে।" to "I have a headache.",
        "তোমার মাথাব্যথা আছে?" to "Do you have a headache?","আমার কাজ আছে।" to "I have work.","তোমার কাজ আছে।" to "Do you have work?",
        "এখন কাজ করো।" to "Work now.","আজ কাজ আছে।" to "There is work today.","হ্যাঁ।" to "Yes.","না।" to "No.","ধন্যবাদ।" to "Thank you."
    )

    val BANGLA_WORD_TO_ENGLISH = mapOf(
        "আমি" to "I","তুমি" to "you","ভালো" to "good","খারাপ" to "bad","সাহায্য" to "help","অপেক্ষা" to "wait",
        "অপেক্ষা করো" to "wait","চুপ" to "quiet","চুপ করো" to "quiet","দুঃখিত" to "sorry","কে" to "who","কখন" to "when",
        "ক্ষুধার্ত" to "hungry","ক্লান্ত" to "tired","মাথাব্যথা" to "headache","কাজ" to "work","আমার" to "my","তোমার" to "your",
        "এখন" to "now","আজ" to "today","আজে" to "today","হ্যাঁ" to "yes","না" to "no","ধন্যবাদ" to "thank you"
    )

    val ENGLISH_SENTENCE_TO_SIGNWRITING = mapOf(
        "Help please." to "𝡌 𝢌","Please wait." to "𝡌 𝡦","I am sorry." to "𝡊𝢌","Be quiet." to "𝠀","Who?" to "𝠆","When?" to "𝠅",
        "It's good." to "𝡒","It's bad." to "𝡌𝪛","I am fine." to "𝡊 𝡒","I am not well." to "𝡌𝪛","I am hungry." to "𝡨","I am tired." to "𝡽",
        "Are you okay?" to "𝡒","Are you not well?" to "𝡌𝪛","Are you hungry?" to "𝡨","Are you tired?" to "𝡽",
        "I have a headache." to "𝧿","Do you have a headache?" to "𝧿","I have work." to "𝢌","Do you have work?" to "𝢌",
        "Work now." to "𝢌 𝢚","There is work today." to "𝢚 𝢌","Yes." to "𝢞","No." to "𝢟","Thank you." to "🙏"
    )

    val ENGLISH_SENTENCE_TO_SPANISH = mapOf(
        "Help please." to "Ayuda por favor.","Please wait." to "Por favor, espera.","I am sorry." to "Lo siento.",
        "Be quiet." to "Silencio.","Who?" to "¿Quién?","When?" to "¿Cuándo?","It's good." to "Es bueno.","It's bad." to "Es malo.",
        "I am fine." to "Estoy bien.","I am not well." to "No estoy bien.","I am hungry." to "Tengo hambre.",
        "I am tired." to "Estoy cansado.","Are you okay?" to "¿Estás bien?","Are you not well?" to "¿No estás bien?",
        "Are you hungry?" to "¿Tienes hambre?","Are you tired?" to "¿Estás cansado?","I have a headache." to "Tengo dolor de cabeza.",
        "Do you have a headache?" to "¿Tienes dolor de cabeza?","I have work." to "Tengo trabajo.","Do you have work?" to "¿Tienes trabajo?",
        "Work now." to "Trabaja ahora.","There is work today." to "Hay trabajo hoy.","Yes." to "Sí.","No." to "No.","Thank you." to "Gracias."
    )

    val ENGLISH_SENTENCE_TO_EMOJI = mapOf(
        "Help please." to "🆘","Please wait." to "⏳","I am sorry." to "😔","Be quiet." to "🤫","Who?" to "🤔","When?" to "❓",
        "It's good." to "👍","It's bad." to "👎","I am fine." to "😊","I am not well." to "🤒","I am hungry." to "🍔","I am tired." to "😫",
        "Are you okay?" to "🙂","Are you not well?" to "🤨","Are you hungry?" to "🤔","Are you tired?" to "🥱","I have a headache." to "🤕",
        "Do you have a headache?" to "🤕","I have work." to "💼","Do you have work?" to "💼","Work now." to "🏃‍♂️","There is work today." to "📅",
        "Yes." to "✅","No." to "❌","Thank you." to "🙏"
    )

    val ENGLISH_WORD_TO_EMOJI = mapOf(
        "help" to "🆘","wait" to "⏳","sorry" to "😔","quiet" to "🤫","who" to "🤔","when" to "⏰","good" to "👍","bad" to "👎",
        "hungry" to "🍽️","tired" to "😴","headache" to "🤕","work" to "💼","now" to "⏱️","today" to "📅","i" to "🙋","you" to "👉",
        "yes" to "✅","no" to "❌","thank you" to "🙏"
    )

    val ENGLISH_WORD_TO_SIGNWRITING = mapOf(
        "Good" to "𝡒","Bad" to "𝡌𝪛","Help" to "𝡌","Wait" to "𝡦","Sorry" to "𝢌","Quiet" to "𝠀","Who" to "𝠆","When" to "𝠅",
        "Headache" to "𝧿","Work" to "𝢌","Today" to "𝢚","Now" to "𝢚","I" to "𝡊","You" to "𝡋","Hungry" to "𝡨","Tired" to "𝡽",
        "Fine" to "𝡒","Okay" to "𝡒","Yes" to "𝢞","No" to "𝢟","Thank you" to "🙏"
    )
}
