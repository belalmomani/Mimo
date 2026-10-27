package com.mimo.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mimo.companion.Graph
import com.mimo.companion.R
import com.mimo.companion.domain.dialogue.Dialect
import com.mimo.companion.domain.dialogue.InitiativeMode
import com.mimo.companion.domain.dialogue.Lang
import com.mimo.companion.speech.VoiceStatus
import java.util.Locale

/** "شخصيتي وقواعدي" - first slice: voice, text and initiative/quiet rules. Every switch works on its own. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val s by Graph.settings.flow.collectAsState()
    val voice by Graph.speech.status.collectAsState()
    val d = s.dialogue

    Column(modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        Section(R.string.settings_voice) {
            SwitchRow(R.string.settings_voice_on, s.voiceEnabled) { v -> Graph.settings.update { it.copy(voiceEnabled = v) }; if (!v) Graph.speech.stop() }
            SwitchRow(R.string.settings_show_text, s.showText) { v -> Graph.settings.update { it.copy(showText = v) } }
            Text(stringResource(R.string.settings_lang), style = MaterialTheme.typography.labelLarge)
            Radio(listOf(Lang.AR to "العربية", Lang.EN to "English"), d.lang) { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(lang = v)) } }
            Text(stringResource(R.string.settings_dialect), style = MaterialTheme.typography.labelLarge)
            Radio(listOf(Dialect.NEUTRAL to stringResource(R.string.dialect_neutral), Dialect.JORDANIAN to stringResource(R.string.dialect_jordanian)), d.dialect) { v ->
                Graph.settings.update { it.copy(dialogue = it.dialogue.copy(dialect = v)) }
            }
            Text(stringResource(R.string.settings_rate, String.format(Locale.US, "%.1f", s.speechRate)), style = MaterialTheme.typography.labelLarge)
            Slider(s.speechRate, { v -> Graph.settings.update { it.copy(speechRate = (Math.round(v * 10) / 10f).coerceIn(0.5f, 1.5f)) } }, valueRange = 0.5f..1.5f)
            Button(onClick = { Thread { Graph.testVoice() }.start() }) { Text(stringResource(R.string.settings_test_voice)) }
            when (voice) {
                VoiceStatus.LANGUAGE_MISSING -> Text(stringResource(R.string.voice_language_missing), color = MaterialTheme.colorScheme.error)
                VoiceStatus.UNAVAILABLE -> Text(stringResource(R.string.voice_unavailable), color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }

        Section(R.string.settings_behaviour) {
            Text(stringResource(R.string.settings_initiative), style = MaterialTheme.typography.labelLarge)
            Radio(
                listOf(InitiativeMode.QUIET to stringResource(R.string.mode_quiet), InitiativeMode.SOCIAL to stringResource(R.string.mode_social)),
                d.mode.takeIf { it != InitiativeMode.CUSTOM } ?: InitiativeMode.SOCIAL,
            ) { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(mode = v)) } }
            Text(stringResource(R.string.settings_max_hour, d.maxProactivePerHour), style = MaterialTheme.typography.labelLarge)
            Slider(d.maxProactivePerHour.toFloat(), { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(maxProactivePerHour = v.toInt())) } }, valueRange = 0f..10f, steps = 9)
            Text(stringResource(R.string.settings_min_gap, d.minSecondsBetweenProactive / 60), style = MaterialTheme.typography.labelLarge)
            Slider((d.minSecondsBetweenProactive / 60).toFloat(), { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(minSecondsBetweenProactive = v.toInt() * 60)) } }, valueRange = 1f..120f)
        }

        Section(R.string.settings_quiet) {
            Text(stringResource(R.string.settings_quiet_from, d.quietStartMin / 60), style = MaterialTheme.typography.labelLarge)
            Slider((d.quietStartMin / 60).toFloat(), { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(quietStartMin = v.toInt() * 60)) } }, valueRange = 0f..23f, steps = 22)
            Text(stringResource(R.string.settings_quiet_to, d.quietEndMin / 60), style = MaterialTheme.typography.labelLarge)
            Slider((d.quietEndMin / 60).toFloat(), { v -> Graph.settings.update { it.copy(dialogue = it.dialogue.copy(quietEndMin = v.toInt() * 60)) } }, valueRange = 0f..23f, steps = 22)
            Text(stringResource(R.string.settings_quiet_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label)); Switch(checked, onChange)
    }
}

@Composable
private fun <T> Radio(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().selectable(selected == value, onClick = { onSelect(value) }, role = Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == value, onClick = null); Text(label, Modifier.padding(start = 8.dp))
            }
        }
    }
}
