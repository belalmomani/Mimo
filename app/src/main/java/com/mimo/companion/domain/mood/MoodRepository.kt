package com.mimo.companion.domain.mood

import com.mimo.companion.mood.MoodStore
import java.time.LocalDate
import java.util.UUID

class MoodRepository(private val store: MoodStore) {
    private var cache = store.getAll().toMutableList()

    fun all() = cache.toList()
    fun todayEntry() = cache.find { it.day == LocalDate.now().toString() }
    fun loggedToday() = todayEntry() != null
    fun lastDays(n: Int): List<MoodEntry> {
        val days = (0 until n).map { LocalDate.now().minusDays(it.toLong()).toString() }
        return days.mapNotNull { day -> cache.find { it.day == day } }
    }
    fun currentMood() = todayEntry()?.mood
    fun logToday(mood: MoodLevel, note: String = "") {
        cache.removeAll { it.day == LocalDate.now().toString() }
        cache.add(MoodEntry(UUID.randomUUID().toString(), LocalDate.now().toString(), mood, note, System.currentTimeMillis()))
        store.save(cache)
    }
}
