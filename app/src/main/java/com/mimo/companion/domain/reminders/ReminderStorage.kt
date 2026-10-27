package com.mimo.companion.domain.reminders

interface ReminderStorage {
    fun all(): List<Reminder>
    fun get(id: String): Reminder?
    fun upsert(r: Reminder)
    fun delete(id: String)
    fun completedDays(id: String): Set<String>
    fun addCompletion(id: String, dayKey: String)
    fun snoozedUntil(id: String): Long
    fun setSnoozedUntil(id: String, millis: Long)
}
