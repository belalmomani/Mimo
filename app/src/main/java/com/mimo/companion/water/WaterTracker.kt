package com.mimo.companion.water

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks daily water intake (in glasses).
 * Resets at midnight automatically on first access of a new day.
 */
class WaterTracker(context: Context) {

    private val prefs = context.getSharedPreferences("mimo_water", Context.MODE_PRIVATE)

    private val _glasses = MutableStateFlow(loadToday())
    val glasses: StateFlow<Int> get() = _glasses

    /** Target glasses per day (default 8). */
    var dailyGoal: Int
        get() = prefs.getInt("goal", 8)
        set(v) { prefs.edit().putInt("goal", v).apply() }

    /** Add one glass and persist. */
    fun drink() {
        ensureTodayReset()
        val next = _glasses.value + 1
        _glasses.value = next
        prefs.edit().putInt(todayKey(), next).apply()
    }

    /** Remove one glass (undo). */
    fun undo() {
        ensureTodayReset()
        val next = maxOf(0, _glasses.value - 1)
        _glasses.value = next
        prefs.edit().putInt(todayKey(), next).apply()
    }

    /** True if the daily goal is reached. */
    fun goalReached(): Boolean = _glasses.value >= dailyGoal

    private fun loadToday(): Int {
        ensureTodayReset()
        return prefs.getInt(todayKey(), 0)
    }

    private fun ensureTodayReset() {
        val today = todayKey()
        val last  = prefs.getString("last_day", "")
        if (last != today) {
            prefs.edit().putString("last_day", today).putInt(today, 0).apply()
            _glasses.value = 0
        }
    }

    private fun todayKey(): String = java.time.LocalDate.now().toString()
}
