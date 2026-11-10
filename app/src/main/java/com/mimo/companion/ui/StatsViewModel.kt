package com.mimo.companion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimo.companion.Graph
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.steps.StepState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayStats(
    val dayLabel: String,   // "Mon", "الإثنين" etc.
    val dayKey: String,     // "2026-11-07"
    val remindersCompleted: Int,
    val prayersLogged: Int, // 0–5
)

data class StatsState(
    val week: List<DayStats> = emptyList(),
    val todaySteps: Int = 0,
    val stepGoal: Int = StepState.DEFAULT_GOAL,
    val totalRemindersEnabled: Int = 0,
    val prayerEnabled: Boolean = false,
)

class StatsViewModel : ViewModel() {
    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> get() = _state

    init { reload() }

    fun reload() = viewModelScope.launch(Dispatchers.IO) {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val prayerStorage = Graph.prayer   // PrayerService
        val reminderStorage = Graph.reminders
        val prayerEnabled = Graph.prayerSettings.value.enabled

        val allReminders = reminderStorage.allWithState()   // List<Pair<Reminder,ReminderState>>
        val week = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val key = date.format(fmt)
            val remDone = allReminders.count { (_, state) -> key in state.completedDays }
            val prayers = if (prayerEnabled) prayerStorage.loggedPrayers(key) else 0
            DayStats(
                dayLabel = date.dayOfWeek.name.take(3)
                    .lowercase().replaceFirstChar { it.uppercase() },
                dayKey = key,
                remindersCompleted = remDone,
                prayersLogged = prayers,
            )
        }

        val steps = Graph.steps.state.value
        _state.value = StatsState(
            week = week,
            todaySteps = if (steps.available) steps.todaySteps else 0,
            stepGoal = steps.goal,
            totalRemindersEnabled = allReminders.count { (r, _) -> r.enabled },
            prayerEnabled = prayerEnabled,
        )
    }
}
