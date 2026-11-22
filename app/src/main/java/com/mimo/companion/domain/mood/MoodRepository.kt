package com.mimo.companion.domain.mood

import java.time.LocalDate

/**
 * Domain-layer façade for mood tracking.
 * All methods are synchronous (called from IO thread or test).
 */
class MoodRepository(private val storage: MoodStorage) {

    /** All entries, newest first. */
    fun all(): List<MoodEntry> = storage.loadAll().sortedByDescending { it.recordedAt }

    /** Entry for today, or null if not logged yet. */
    fun todayEntry(): MoodEntry? {
        val today = todayKey()
        return storage.loadAll().firstOrNull { it.day == today }
    }

    /** True if the user has already logged today. */
    fun loggedToday(): Boolean = todayEntry() != null

    /** Last N days (oldest first) for the chart. */
    fun lastDays(n: Int = 7): List<MoodEntry> {
        val all = storage.loadAll().sortedBy { it.recordedAt }
        return if (all.size <= n) all else all.takeLast(n)
    }

    /** Save or replace today's entry. */
    fun logToday(mood: MoodLevel, note: String = "") {
        val existing = todayEntry()
        if (existing != null) storage.delete(existing.id)
        storage.save(MoodEntry(day = todayKey(), mood = mood, note = note))
    }

    /** Current mood level (today's), or null. */
    fun currentMood(): MoodLevel? = todayEntry()?.mood

    private fun todayKey(): String = LocalDate.now().toString()   // "YYYY-MM-DD"
}
