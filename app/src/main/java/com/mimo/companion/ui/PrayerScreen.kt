package com.mimo.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import com.mimo.companion.Graph
import com.mimo.companion.R
import com.mimo.companion.domain.prayer.AsrMethod
import com.mimo.companion.domain.prayer.CalcMethod
import com.mimo.companion.domain.prayer.Cities
import com.mimo.companion.domain.prayer.Prayer
import java.util.Locale

private fun methodName(m: CalcMethod): Int = when (m) {
    CalcMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
    CalcMethod.EGYPTIAN -> R.string.method_egyptian
    CalcMethod.KARACHI -> R.string.method_karachi
    CalcMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
    CalcMethod.ISNA -> R.string.method_isna
}

private fun prayerLabel(p: Prayer): Int = when (p) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.SUNRISE -> R.string.prayer_sunrise
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
}

/** Prayer times (computed on the device from the chosen city and method), optional reminders and a private log. */
@Composable
fun PrayerScreen(modifier: Modifier = Modifier, vm: PrayerViewModel = viewModel()) {
    val s by Graph.prayerSettings.flow.collectAsState()
    val ui by vm.state.collectAsState()
    val ar = Locale.getDefault().language == "ar"
    var pickCity by remember { mutableStateOf(false) }
    var pickMethod by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val city = Cities.byId(s.cityId)

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.prayer_enable), style = MaterialTheme.typography.titleMedium)
            Switch(s.enabled, { v -> vm.change { it.copy(enabled = v) } })
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickCity = true }) { Text(stringResource(R.string.prayer_city, if (ar) city.ar else city.en)) }
                OutlinedButton(onClick = { pickMethod = true }) { Text(stringResource(R.string.prayer_method, stringResource(methodName(s.method)))) }
                Text(stringResource(R.string.prayer_asr_method), style = MaterialTheme.typography.labelLarge)
                listOf(AsrMethod.STANDARD to R.string.asr_standard, AsrMethod.HANAFI to R.string.asr_hanafi).forEach { (m, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(s.asr == m, onClick = { vm.change { it.copy(asr = m) } }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(s.asr == m, onClick = null)
                        Text(stringResource(label), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.prayer_today, ui.date?.toString().orEmpty()), style = MaterialTheme.typography.titleMedium)
                Prayer.values().forEach { p ->
                    val t = ui.day?.get(p)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(prayerLabel(p)), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            t?.let { String.format(Locale.US, "%02d:%02d", it.hour, it.minute) } ?: stringResource(R.string.prayer_unavailable),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (p.isPrayer && t != null) {
                            val done = p in ui.done
                            TextButton(onClick = { vm.setPrayed(p, !done) }) { Text(stringResource(if (done) R.string.prayer_logged else R.string.prayer_prayed)) }
                        }
                    }
                }
                Text(stringResource(R.string.prayer_disclaimer), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.prayer_tune), style = MaterialTheme.typography.titleMedium)
                Prayer.values().forEach { p ->
                    val adj = s.adjustMin[p] ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(prayerLabel(p)))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { vm.change { it.copy(adjustMin = it.adjustMin + (p to (adj - 1).coerceAtLeast(-30))) } }) { Text("-") }
                            Text(String.format(Locale.US, "%+d", adj))
                            TextButton(onClick = { vm.change { it.copy(adjustMin = it.adjustMin + (p to (adj + 1).coerceAtMost(30))) } }) { Text("+") }
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.prayer_reminders), style = MaterialTheme.typography.titleMedium)
                Prayer.values().filter { it.isPrayer }.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(p in s.remindFor, { on -> vm.change { it.copy(remindFor = if (on) it.remindFor + p else it.remindFor - p) } })
                        Text(stringResource(prayerLabel(p)))
                    }
                }
                Text(stringResource(R.string.prayer_before, s.minutesBefore), style = MaterialTheme.typography.labelLarge)
                Slider(s.minutesBefore.toFloat(), { v -> vm.change { it.copy(minutesBefore = v.toInt().coerceIn(0, 60)) } }, valueRange = 0f..60f)
                Text(stringResource(R.string.prayer_count, s.reminderCount), style = MaterialTheme.typography.labelLarge)
                Slider(s.reminderCount.toFloat(), { v -> vm.change { it.copy(reminderCount = v.toInt().coerceIn(1, 3)) } }, valueRange = 1f..3f, steps = 1)
                Text(stringResource(R.string.prayer_gap, s.gapMinutes), style = MaterialTheme.typography.labelLarge)
                Slider(s.gapMinutes.toFloat(), { v -> vm.change { it.copy(gapMinutes = v.toInt().coerceIn(1, 60)) } }, valueRange = 1f..60f)
                var msg by remember(s.customMessage) { mutableStateOf(s.customMessage.orEmpty()) }
                OutlinedTextField(
                    msg, { msg = it; vm.change { st -> st.copy(customMessage = it.trim().ifBlank { null }) } },
                    label = { Text(stringResource(R.string.prayer_message)) }, modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.prayer_silence_note), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.prayer_privacy), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.prayer_delete_log)) }
            }
        }
    }

    if (pickCity) PickDialog(
        R.string.prayer_pick_city, Cities.ALL.map { it.id to (if (ar) it.ar else it.en) }, s.cityId,
        onPick = { id -> vm.change { it.copy(cityId = id) }; pickCity = false }, onDismiss = { pickCity = false },
    )
    if (pickMethod) {
        val opts = CalcMethod.values().map { it.name to stringResource(methodName(it)) }
        PickDialog(
            R.string.prayer_pick_method, opts, s.method.name,
            onPick = { n -> vm.change { it.copy(method = CalcMethod.valueOf(n)) }; pickMethod = false }, onDismiss = { pickMethod = false },
        )
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.prayer_delete_log)) },
        text = { Text(stringResource(R.string.prayer_delete_confirm)) },
        confirmButton = { TextButton(onClick = { vm.deleteLog(); confirmDelete = false }) { Text(stringResource(R.string.prayer_delete_log)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

@Composable
private fun PickDialog(title: Int, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(id == selected, onClick = { onPick(id) }, role = Role.RadioButton).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(id == selected, onClick = null)
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) } },
    )
}
