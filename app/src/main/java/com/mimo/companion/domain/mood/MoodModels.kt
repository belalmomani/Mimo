package com.mimo.companion.domain.mood

enum class MoodLevel(val emoji: String, val labelAr: String, val score: Int) {
    GREAT("😄", "رائع", 5), GOOD("🙂", "جيد", 4), OKAY("😐", "عادي", 3),
    TIRED("😴", "متعب", 2), STRESSED("😟", "متوتر", 1), SAD("😢", "حزين", 0)
}

data class MoodEntry(
    val id: String, val day: String,
    val mood: MoodLevel, val note: String = "",
    val recordedAt: Long
)
