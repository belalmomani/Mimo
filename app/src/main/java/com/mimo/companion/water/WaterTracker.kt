package com.mimo.companion.water

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WaterTracker(context: Context) {
    private val prefs = context.getSharedPreferences("mimo_water", Context.MODE_PRIVATE)
    private val todayKey get() = java.time.LocalDate.now().toString()

    private val _glasses = MutableStateFlow(prefs.getInt(todayKey, 0))
    val glasses: StateFlow<Int> = _glasses

    var dailyGoal: Int
        get() = prefs.getInt("goal", 8)
        set(v) = prefs.edit().putInt("goal", v).apply()

    fun drink() {
        val next = _glasses.value + 1
        _glasses.value = next
        prefs.edit().putInt(todayKey, next).apply()
    }

    fun undo() {
        val prev = (_glasses.value - 1).coerceAtLeast(0)
        _glasses.value = prev
        prefs.edit().putInt(todayKey, prev).apply()
    }

    fun goalReached() = _glasses.value >= dailyGoal
}
