package com.mimo.companion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mimo.companion.Graph
import com.mimo.companion.domain.reminders.Reminder
import com.mimo.companion.reminders.ReminderAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class ReminderRow(val reminder: Reminder, val doneToday: Boolean, val nextAtMillis: Long?)

class RemindersViewModel(app: Application) : AndroidViewModel(app) {
    private val _rows = MutableStateFlow<List<ReminderRow>>(emptyList())
    val rows: StateFlow<List<ReminderRow>> = _rows

    init { refresh() }

    fun refresh() = io {
        _rows.value = Graph.reminders.list().map { ReminderRow(it, Graph.reminders.isDoneToday(it.id), Graph.reminders.nextFor(it)) }
    }

    fun save(r: Reminder) = io { Graph.reminders.save(r); afterChange() }
    fun delete(id: String) = io { Graph.reminders.delete(id); afterChange() }
    fun setEnabled(id: String, on: Boolean) = io { Graph.reminders.setEnabled(id, on); afterChange() }
    fun done(id: String) = io { Graph.reminders.markDone(id); Graph.onReminderDone(); afterChange() }

    private fun afterChange() { ReminderAlarm.reschedule(getApplication()); refresh() }
    private fun io(block: () -> Unit) { viewModelScope.launch(Dispatchers.IO) { block() } }

    companion object {
        fun todayIso(): String = java.time.LocalDate.now().toString()
        fun tomorrowIso(): String = java.time.LocalDate.now().plusDays(1).toString()
        fun formatTime(min: Int) = "%02d:%02d".format(min / 60, min % 60)
        fun formatNext(ms: Long?): String? = ms?.let {
            val t = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            "%s %02d:%02d".format(t.toLocalDate(), t.hour, t.minute)
        }
    }
}
