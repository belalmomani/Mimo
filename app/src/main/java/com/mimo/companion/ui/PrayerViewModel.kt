package com.mimo.companion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimo.companion.Graph
import com.mimo.companion.domain.prayer.Prayer
import com.mimo.companion.domain.prayer.PrayerDay
import com.mimo.companion.domain.prayer.PrayerSettings
import com.mimo.companion.reminders.ReminderAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PrayerUiState(val date: LocalDate? = null, val day: PrayerDay? = null, val done: Set<Prayer> = emptySet())

class PrayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(PrayerUiState())
    val state: StateFlow<PrayerUiState> = _state

    init { io { recompute() } }

    /** Any settings change: save, re-arm the single alarm, recompute today's table. */
    fun change(update: (PrayerSettings) -> PrayerSettings) = io {
        Graph.prayerSettings.update(update)
        ReminderAlarm.reschedule(Graph.appCtx())
        recompute()
    }

    fun setPrayed(p: Prayer, prayed: Boolean) = io {
        val date = Graph.prayer.dateNow(Graph.prayerSettings.value)
        if (prayed) { Graph.prayer.markPrayed(date, p); Graph.onPrayerLogged() } else Graph.prayer.unmarkPrayed(date, p)
        ReminderAlarm.reschedule(Graph.appCtx())
        recompute()
    }

    /** Deletes the prayer log and pending follow-ups; settings stay. */
    fun deleteLog() = io {
        Graph.prayer.deleteAllData()
        ReminderAlarm.reschedule(Graph.appCtx())
        recompute()
    }

    private fun recompute() {
        val s = Graph.prayerSettings.value
        val date = Graph.prayer.dateNow(s)
        val done = Prayer.values().filter { Graph.prayer.isDone(s, date, it) }.toSet()
        _state.value = PrayerUiState(date, Graph.prayer.dayFor(s, date), done)
    }

    private fun io(block: () -> Unit) { viewModelScope.launch(Dispatchers.IO) { block() } }
}
