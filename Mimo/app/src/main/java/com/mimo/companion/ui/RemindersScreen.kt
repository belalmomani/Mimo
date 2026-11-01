package com.mimo.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mimo.companion.R
import com.mimo.companion.domain.reminders.Delivery
import com.mimo.companion.domain.reminders.Reminder
import com.mimo.companion.domain.reminders.RepeatType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

/** Habits & reminders: add anything (water, sleep, study, plants, pets, a medication appointment...). It schedules; it gives no medical advice. */
@Composable
fun RemindersScreen(modifier: Modifier = Modifier, vm: RemindersViewModel = viewModel()) {
    val rows by vm.rows.collectAsState()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var creating by remember { mutableStateOf<String?>(null) } // template name or "" for blank

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { creating = "" }) { Text(stringResource(R.string.reminder_add)) }
        TemplateChips { creating = it }
        if (rows.isEmpty()) Text(stringResource(R.string.reminders_empty), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.reminder.id }) { row ->
                val r = row.reminder
                Card(Modifier.fillMaxWidth().clickable { editing = r }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium)
                                Text(RemindersViewModel.formatTime(r.timeMin) + " · " + repeatSummary(r), style = MaterialTheme.typography.bodySmall)
                                RemindersViewModel.formatNext(row.nextAtMillis)?.let { Text(stringResource(R.string.reminder_next, it), style = MaterialTheme.typography.labelSmall) }
                            }
                            Switch(r.enabled, { vm.setEnabled(r.id, it) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (row.doneToday) Text(stringResource(R.string.reminder_done_today), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            else OutlinedButton(onClick = { vm.done(r.id) }, enabled = r.enabled) { Text(stringResource(R.string.reminder_mark_done)) }
                            TextButton(onClick = { vm.delete(r.id) }) { Text(stringResource(R.string.reminder_delete), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    val base = editing
    if (base != null || creating != null) {
        ReminderEditor(
            initial = base ?: Reminder(UUID.randomUUID().toString(), "-", timeMin = 9 * 60), // placeholder name: a new reminder starts with an empty field
            initialName = base?.name ?: creating.orEmpty(),
            isNew = base == null,
            onDismiss = { editing = null; creating = null },
            onSave = { vm.save(it); editing = null; creating = null },
        )
    }
}

@Composable
private fun repeatSummary(r: Reminder): String = when (r.repeat) {
    RepeatType.ONCE -> stringResource(R.string.repeat_once) + " (" + r.onceDate + ")"
    RepeatType.DAILY -> stringResource(R.string.repeat_daily)
    RepeatType.WEEKLY -> r.days.sortedBy { it.value }.joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TemplateChips(onPick: (String) -> Unit) {
    val names = listOf(R.string.tpl_water, R.string.tpl_sleep, R.string.tpl_wake, R.string.tpl_study, R.string.tpl_work, R.string.tpl_plants, R.string.tpl_pets, R.string.tpl_medication)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { id -> val label = stringResource(id); AssistChip(onClick = { onPick(label) }, label = { Text(label) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditor(initial: Reminder, initialName: String, isNew: Boolean, onDismiss: () -> Unit, onSave: (Reminder) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var message by remember { mutableStateOf(initial.customMessage.orEmpty()) }
    var hour by remember { mutableStateOf(initial.timeMin / 60) }
    var minute by remember { mutableStateOf(initial.timeMin % 60) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var days by remember { mutableStateOf(initial.days) }
    var delivery by remember { mutableStateOf(initial.delivery) }
    var snooze by remember { mutableStateOf(initial.snoozeMinutes) }

    val valid = name.isNotBlank() && delivery.isNotEmpty() && (repeat != RepeatType.WEEKLY || days.isNotEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.reminder_add else R.string.reminder_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.reminder_name)) }, singleLine = true)
                Text(stringResource(R.string.reminder_time, RemindersViewModel.formatTime(hour * 60 + minute)), style = MaterialTheme.typography.labelLarge)
                Slider(hour.toFloat(), { hour = it.toInt() }, valueRange = 0f..23f, steps = 22)
                Slider(minute.toFloat(), { minute = it.toInt() }, valueRange = 0f..59f, steps = 58)
                listOf(RepeatType.ONCE to R.string.repeat_once, RepeatType.DAILY to R.string.repeat_daily, RepeatType.WEEKLY to R.string.repeat_weekly).forEach { (t, label) ->
                    Row(Modifier.fillMaxWidth().selectable(repeat == t, onClick = { repeat = t }, role = Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(repeat == t, onClick = null); Text(stringResource(label), Modifier.padding(start = 8.dp))
                    }
                }
                if (repeat == RepeatType.WEEKLY) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.values().forEach { d ->
                        FilterChip(d in days, { days = if (d in days) days - d else days + d }, label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                    }
                }
                Text(stringResource(R.string.reminder_how), style = MaterialTheme.typography.labelLarge)
                listOf(Delivery.NOTIFICATION to R.string.delivery_notification, Delivery.SPEECH to R.string.delivery_speech, Delivery.VISUAL to R.string.delivery_visual).forEach { (d, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(d in delivery, { delivery = if (it) delivery + d else delivery - d }); Text(stringResource(label))
                    }
                }
                Text(stringResource(R.string.delivery_note), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(message, { message = it }, label = { Text(stringResource(R.string.reminder_message)) })
                Text(stringResource(R.string.reminder_snooze_len, snooze), style = MaterialTheme.typography.labelLarge)
                Slider(snooze.toFloat(), { snooze = it.toInt().coerceAtLeast(1) }, valueRange = 1f..60f)
                Text(stringResource(R.string.reminders_not_medical), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val t = hour * 60 + minute
                val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                val once = if (t > nowMin) RemindersViewModel.todayIso() else RemindersViewModel.tomorrowIso()
                onSave(
                    initial.copy(
                        name = name.trim(), timeMin = t, repeat = repeat, days = if (repeat == RepeatType.WEEKLY) days else emptySet(),
                        onceDate = if (repeat == RepeatType.ONCE) (if (initial.repeat == RepeatType.ONCE && !isNew) initial.onceDate ?: once else once) else null,
                        customMessage = message.trim().ifBlank { null }, delivery = delivery, snoozeMinutes = snooze, enabled = true,
                    ),
                )
            }) { Text(stringResource(R.string.reminder_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}
