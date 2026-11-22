package com.mimo.companion.domain.mood

/**
 * The user's mood for a given day.
 */
enum class MoodLevel(val emoji: String, val labelAr: String, val score: Int) {
    GREAT("😄", "رائع", 5),
    GOOD("🙂", "جيد", 4),
    OKAY("😐", "عادي", 3),
    TIRED("😴", "متعب", 2),
    STRESSED("😟", "متوتر", 1),
    SAD("😢", "حزين", 0),
}

data class MoodEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    /** Day key: "YYYY-MM-DD" */
    val day: String,
    val mood: MoodLevel,
    val note: String = "",
    val recordedAt: Long = System.currentTimeMillis() / 1_000L,
)
