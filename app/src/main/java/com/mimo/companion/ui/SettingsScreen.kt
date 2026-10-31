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
import com.mimo.companion.domain.dialogue.VoiceSource
import com.mimo.companion.speech.AddClipResult
import com.mimo.companion.speech.CustomVoiceStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import android.widget.Toast
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

        MyVoiceSection(s.voiceSource)

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

/** The user's own voice clips. Adding clips never removes the built-in voice; the source choice decides what plays. */
@Composable
private fun MyVoiceSection(source: VoiceSource) {
    val ctx = LocalContext.current
    val clips by Graph.customVoices.clips.collectAsState()
    var msgRes by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Thread {
            val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "clip"
            val res = Graph.customVoices.add(uri, name)
            msgRes = when (res) {
                is AddClipResult.Added -> R.string.myvoice_added
                AddClipResult.TooMany -> R.string.myvoice_too_many
                AddClipResult.TooBig -> R.string.myvoice_too_big
                AddClipResult.TooLong -> R.string.myvoice_too_long
                AddClipResult.NotAudio -> R.string.myvoice_not_audio
                is AddClipResult.Failed -> R.string.myvoice_failed
            }
        }.start()
    }
    Section(R.string.myvoice_title) {
        Text(stringResource(R.string.myvoice_hint, CustomVoiceStore.MAX_CLIPS, CustomVoiceStore.MAX_MS / 1000, CustomVoiceStore.MAX_BYTES / (1024 * 1024)), style = MaterialTheme.typography.bodySmall)
        Radio(
            listOf(
                VoiceSource.TTS_ONLY to stringResource(R.string.myvoice_tts_only),
                VoiceSource.CLIPS_ONLY to stringResource(R.string.myvoice_clips_only),
                VoiceSource.CLIP_THEN_TTS to stringResource(R.string.myvoice_both),
            ),
            source,
        ) { v -> Graph.settings.update { it.copy(voiceSource = v) } }
        Text(stringResource(R.string.myvoice_keep_note), style = MaterialTheme.typography.bodySmall)
        clips.forEach { c ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.name, Modifier.weight(1f))
                TextButton(onClick = { Graph.speech.playClip(c.file, null) { false } }) { Text(stringResource(R.string.myvoice_play)) }
                TextButton(onClick = { Graph.customVoices.remove(c.id) }) { Text(stringResource(R.string.myvoice_delete)) }
            }
        }
        OutlinedButton(enabled = clips.size < CustomVoiceStore.MAX_CLIPS, onClick = { picker.launch(arrayOf("audio/*")) }) { Text(stringResource(R.string.myvoice_add)) }
        msgRes?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    }
}
