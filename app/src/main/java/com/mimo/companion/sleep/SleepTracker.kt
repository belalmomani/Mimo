package com.mimo.companion.sleep

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SleepEntry(
    val date: String,           // "YYYY-MM-DD" (the morning date)
    val bedtimeMinutes: Int,    // minutes from midnight (e.g. 23*60+30 = 1410)
    val wakeMinutes: Int,       // minutes from midnight (e.g. 7*60 = 420)
) {
    /** Duration in minutes (handles crossing midnight). */
    val durationMinutes: Int get() {
        val d = wakeMinutes - bedtimeMinutes
        return if (d < 0) d + 24 * 60 else d
    }
    val durationHours: Float get() = durationMinutes / 60f

    fun formatBedtime()  = "%02d:%02d".format(bedtimeMinutes / 60, bedtimeMinutes % 60)
    fun formatWake()     = "%02d:%02d".format(wakeMinutes / 60, wakeMinutes % 60)
    fun formatDuration() = "%.1f ساعة".format(durationHours)
}

/**
 * Stores and loads sleep log entries from SharedPreferences.
 * Also exposes user-configurable bedtime/wake reminders.
 */
class SleepTracker(context: Context) {

    private val prefs = context.getSharedPreferences("mimo_sleep", Context.MODE_PRIVATE)

    // ── Reminder settings ────────────────────────────────────────────────────

    /** Bedtime reminder hour (24h). Default 22 (10 PM). */
    var bedtimeHour: Int
        get() = prefs.getInt("bedtime_h", 22)
        set(v) { prefs.edit().putInt("bedtime_h", v).apply(); _bedtimeHour.value = v }

    var bedtimeMin: Int
        get() = prefs.getInt("bedtime_m", 0)
        set(v) { prefs.edit().putInt("bedtime_m", v).apply(); _bedtimeMin.value = v }

    var wakeHour: Int
        get() = prefs.getInt("wake_h", 6)
        set(v) { prefs.edit().putInt("wake_h", v).apply(); _wakeHour.value = v }

    var wakeMin: Int
        get() = prefs.getInt("wake_m", 0)
        set(v) { prefs.edit().putInt("wake_m", v).apply(); _wakeMin.value = v }

    private val _bedtimeHour = MutableStateFlow(prefs.getInt("bedtime_h", 22))
    private val _bedtimeMin  = MutableStateFlow(prefs.getInt("bedtime_m", 0))
    private val _wakeHour    = MutableStateFlow(prefs.getInt("wake_h", 6))
    private val _wakeMin     = MutableStateFlow(prefs.getInt("wake_m", 0))

    val bedtimeHourFlow: StateFlow<Int> get() = _bedtimeHour
    val bedtimeMinFlow:  StateFlow<Int> get() = _bedtimeMin
    val wakeHourFlow:    StateFlow<Int> get() = _wakeHour
    val wakeMinFlow:     StateFlow<Int> get() = _wakeMin

    // ── Log ──────────────────────────────────────────────────────────────────

    fun logSleep(bedH: Int, bedM: Int, wakeH: Int, wakeM: Int) {
        val today = java.time.LocalDate.now().toString()
        val entry = SleepEntry(today, bedH * 60 + bedM, wakeH * 60 + wakeM)
        prefs.edit().putString("sleep_$today", serialize(entry)).apply()
        _entries.value = loadAll()
    }

    private val _entries = MutableStateFlow(loadAll())
    val entries: StateFlow<List<SleepEntry>> get() = _entries

    fun lastDays(n: Int = 7): List<SleepEntry> =
        loadAll().sortedBy { it.date }.takeLast(n)

    fun todayEntry(): SleepEntry? {
        val today = java.time.LocalDate.now().toString()
        return prefs.getString("sleep_$today", null)?.let { deserialize(it) }
    }

    private fun loadAll(): List<SleepEntry> =
        prefs.all.keys
            .filter { it.startsWith("sleep_") }
            .mapNotNull { prefs.getString(it, null)?.let { v -> deserialize(v) } }
            .sortedByDescending { it.date }

    private fun serialize(e: SleepEntry) = "${e.date}|${e.bedtimeMinutes}|${e.wakeMinutes}"
    private fun deserialize(s: String): SleepEntry? = runCatching {
        val p = s.split("|")
        SleepEntry(p[0], p[1].toInt(), p[2].toInt())
    }.getOrNull()
}
