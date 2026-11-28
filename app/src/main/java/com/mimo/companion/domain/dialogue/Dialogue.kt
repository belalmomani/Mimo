package com.mimo.companion.domain.dialogue

data class DialogueDecision(val text: String)

class Dialogue {
    private val replies = listOf(
        "كيف يمكنني مساعدتك؟ 😊",
        "أنا هنا معك دائماً!",
        "هل أنت بخير؟ 🌟",
        "يوم رائع بانتظارك!",
        "لا تنسَ أن تأخذ استراحة 🌿",
        "أنت تقوم بعمل رائع!",
        "فكرة رائعة! استمر 💪",
        "خذ نفساً عميقاً، كل شيء سيكون بخير ✨"
    )
    fun reply(): DialogueDecision = DialogueDecision(replies.random())
}
