package com.mimo.companion.domain.prayer

interface PrayerStorage {
    fun doneSet(dayKey: String): Set<Prayer>
    fun setDone(dayKey: String, prayer: Prayer, done: Boolean)
    fun state(dayKey: String, prayer: Prayer): PrayerState
    fun saveState(dayKey: String, prayer: Prayer, state: PrayerState)
    /** Housekeeping: drop log rows older than the given day key (yyyy-MM-dd). */
    fun pruneBefore(dayKey: String)
    /** The user wants all prayer logs / reminder state deleted. */
    fun clearAll()
}
