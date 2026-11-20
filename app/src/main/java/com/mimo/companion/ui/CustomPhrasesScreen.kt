package com.mimo.companion.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mimo.companion.Graph
import com.mimo.companion.domain.custom.CustomPhrase
import com.mimo.companion.domain.custom.PhraseEvent
import com.mimo.companion.speech.AddClipResult
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "عباراتي" – user-created phrases linked to events and optional voice clips.
 *
 * Layout:
 *  ┌─────────────────────────────┐
 *  │  + إضافة عبارة جديدة        │  ← FAB-style button
 *  ├─────────────────────────────┤
 *  │  [Card per phrase]           │
 *  │  • نص العبارة                │
 *  │  • الحدث (صلاة / نوم / …)   │
 *  │  • الصوت المرتبط             │
 *  │  • تشغيل | حذف              │
 *  └─────────────────────────────┘
 */
@Composable
fun CustomPhrasesScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clips by Graph.customVoices.clips.collectAsState()

    // Mutable list backed by the repository
    var phrases by remember { mutableStateOf(Graph.customPhrases.all()) }

    fun refresh() { phrases = Graph.customPhrases.all() }

    // ── Add-phrase dialog state ──────────────────────────────────────────────
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("عباراتي", style = MaterialTheme.typography.headlineMedium)
        Text(
            "أضف عبارات مخصصة واربطها بصوت وحدث. ميمو سيستخدمها بدلاً من العبارات الافتراضية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("إضافة عبارة جديدة")
        }

        if (phrases.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عبارات بعد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(phrases, key = { it.id }) { phrase ->
                    PhraseCard(
                        phrase = phrase,
                        clipName = clips.firstOrNull { it.id == phrase.clipId }?.name,
                        onDelete = {
                            Graph.customPhrases.remove(phrase.id)
                            refresh()
                        },
                        onPlay = { Graph.presentCustom(phrase) },
                        onToggle = { enabled ->
                            Graph.customPhrases.setEnabled(phrase.id, enabled)
                            refresh()
                        },
                    )
                }
            }
        }
    }

    // ── Add dialog ───────────────────────────────────────────────────────────
    if (showAdd) {
        AddPhraseDialog(
            clips = clips.map { it.id to it.name },
            onDismiss = { showAdd = false },
            onAdd = { phrase ->
                Graph.customPhrases.add(phrase)
                refresh()
                showAdd = false
                Toast.makeText(ctx, "تمت الإضافة ✓", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

// ── Phrase card ───────────────────────────────────────────────────────────────

@Composable
private fun PhraseCard(
    phrase: CustomPhrase,
    clipName: String?,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (phrase.enabled) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    phrase.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = phrase.enabled, onCheckedChange = onToggle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventChip(phrase.event, phrase.scheduleHour, phrase.scheduleMin)
                if (clipName != null) {
                    SuggestionChip(onClick = {}, label = { Text("🎙 $clipName", style = MaterialTheme.typography.labelSmall) })
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تشغيل")
                }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("حذف")
                }
            }
        }
    }
}

@Composable
private fun EventChip(event: PhraseEvent, hour: Int, minute: Int) {
    val label = when (event) {
        PhraseEvent.NONE     -> "بلا حدث"
        PhraseEvent.TAP      -> "🖐 نقرة"
        PhraseEvent.PRAYER   -> "🕌 صلاة"
        PhraseEvent.SLEEP    -> "🌙 نوم"
        PhraseEvent.REMINDER -> "🔔 تذكير"
        PhraseEvent.SCHEDULE -> "⏰ " + scheduleLabel(hour, minute)
    }
    SuggestionChip(onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
}

private fun scheduleLabel(hour: Int, minute: Int): String {
    if (hour < 0 || minute < 0) return "وقت مجدول"
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}

// ── Add dialog ───────────────────────────────────────────────────────────────

@Composable
private fun AddPhraseDialog(
    clips: List<Pair<String, String>>,   // id to displayName
    onDismiss: () -> Unit,
    onAdd: (CustomPhrase) -> Unit,
) {
    var text       by remember { mutableStateOf("") }
    var event      by remember { mutableStateOf(PhraseEvent.NONE) }
    var clipId     by remember { mutableStateOf<String?>(null) }
    var schedHour  by remember { mutableStateOf(22) }
    var schedMin   by remember { mutableStateOf(0) }
    var clipExpand by remember { mutableStateOf(false) }
    var eventExpand by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Audio picker for adding a new clip on-the-fly
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "clip"
        scope.launch(Dispatchers.IO) {
            when (val r = Graph.customVoices.add(uri, name)) {
                is AddClipResult.Added -> clipId = r.clip.id
                else -> { /* show nothing; the user can retry */ }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عبارة جديدة") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Text ──────────────────────────────────────────────────────
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("نص العبارة") },
                    placeholder = { Text("مثال: حان وقت الصلاة يا بطل 🕌") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )

                // ── Event ─────────────────────────────────────────────────────
                Text("الحدث", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(expanded = eventExpand, onExpandedChange = { eventExpand = it }) {
                    OutlinedTextField(
                        value = eventLabel(event),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("متى تقوله ميمو؟") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(eventExpand) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = eventExpand, onDismissRequest = { eventExpand = false }) {
                        PhraseEvent.entries.forEach { e ->
                            DropdownMenuItem(
                                text = { Text(eventLabel(e)) },
                                onClick = { event = e; eventExpand = false },
                            )
                        }
                    }
                }

                // ── Schedule time (only for SCHEDULE / SLEEP) ─────────────────
                if (event == PhraseEvent.SCHEDULE || event == PhraseEvent.SLEEP) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = schedHour.toString(),
                            onValueChange = { schedHour = it.toIntOrNull()?.coerceIn(0, 23) ?: schedHour },
                            label = { Text("ساعة (0-23)") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = schedMin.toString(),
                            onValueChange = { schedMin = it.toIntOrNull()?.coerceIn(0, 59) ?: schedMin },
                            label = { Text("دقيقة (0-59)") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── Voice clip ────────────────────────────────────────────────
                Text("الصوت المرتبط (اختياري)", style = MaterialTheme.typography.labelMedium)
                if (clips.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = clipExpand, onExpandedChange = { clipExpand = it }) {
                        OutlinedTextField(
                            value = clips.firstOrNull { it.first == clipId }?.second ?: "بدون صوت مخصص",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختر مقطع صوتي") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(clipExpand) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = clipExpand, onDismissRequest = { clipExpand = false }) {
                            DropdownMenuItem(
                                text = { Text("بدون صوت مخصص") },
                                onClick = { clipId = null; clipExpand = false },
                            )
                            clips.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { clipId = id; clipExpand = false },
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ رفع مقطع صوتي جديد")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isBlank()) {
                        Toast.makeText(ctx, "الرجاء كتابة نص العبارة", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onAdd(CustomPhrase(
                        text         = text.trim(),
                        clipId       = clipId,
                        event        = event,
                        scheduleHour = if (event == PhraseEvent.SCHEDULE || event == PhraseEvent.SLEEP) schedHour else -1,
                        scheduleMin  = if (event == PhraseEvent.SCHEDULE || event == PhraseEvent.SLEEP) schedMin  else -1,
                    ))
                },
            ) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

private fun eventLabel(event: PhraseEvent) = when (event) {
    PhraseEvent.NONE     -> "بلا حدث (يدوي)"
    PhraseEvent.TAP      -> "🖐 عند النقر على ميمو"
    PhraseEvent.PRAYER   -> "🕌 عند وقت الصلاة"
    PhraseEvent.SLEEP    -> "🌙 وقت النوم"
    PhraseEvent.REMINDER -> "🔔 عند أي تذكير"
    PhraseEvent.SCHEDULE -> "⏰ وقت محدد كل يوم"
}
