package com.mimo.companion.sleep

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SleepEntry(val date: String, val bedtimeMinutes: Int, val wakeMinutes: Int) {
    val durationMinutes: Int get() {
        val d = wakeMinutes - bedtimeMinutes
        return if (d < 0) d + 24 * 60 else d
    }
}

class SleepTracker(context: Context) {
    private val prefs = context.getSharedPreferences("mimo_sleep", Context.MODE_PRIVATE)

    var bedtimeHour: Int
        get() = prefs.getInt("bedH", 22); set(v) = prefs.edit().putInt("bedH", v).apply()
    var bedtimeMin: Int
        get() = prefs.getInt("bedM", 30); set(v) = prefs.edit().putInt("bedM", v).apply()
    var wakeHour: Int
        get() = prefs.getInt("wakeH", 6); set(v) = prefs.edit().putInt("wakeH", v).apply()
    var wakeMin: Int
        get() = prefs.getInt("wakeM", 30); set(v) = prefs.edit().putInt("wakeM", v).apply()

    private val _entries = MutableStateFlow(loadEntries())
    val entries: StateFlow<List<SleepEntry>> = _entries

    private fun loadEntries(): List<SleepEntry> {
        val raw = prefs.getString("entries", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("§").mapNotNull {
            val p = it.split("|")
            if (p.size < 3) null
            else try { SleepEntry(p[0], p[1].toInt(), p[2].toInt()) } catch (e: Exception) { null }
        }
    }

    fun logSleep(bedH: Int, bedM: Int, wakeH: Int, wakeM: Int) {
        val today = java.time.LocalDate.now().toString()
        val current = _entries.value.toMutableList()
        current.removeAll { it.date == today }
        current.add(SleepEntry(today, bedH * 60 + bedM, wakeH * 60 + wakeM))
        val raw = current.joinToString("§") { "${it.date}|${it.bedtimeMinutes}|${it.wakeMinutes}" }
        prefs.edit().putString("entries", raw).apply()
        _entries.value = current
    }

    fun todayEntry() = _entries.value.find { it.date == java.time.LocalDate.now().toString() }
    fun lastDays(n: Int) = _entries.value.takeLast(n)
}
