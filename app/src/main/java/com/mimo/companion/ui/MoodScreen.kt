package com.mimo.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.domain.mood.MoodEntry
import com.mimo.companion.domain.mood.MoodLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "مزاجي اليومي" — Mood tracking screen.
 *
 * Shows:
 * 1. Today's mood picker (if not logged yet, or to change it)
 * 2. Weekly bar chart (last 7 days)
 * 3. History list
 */
@Composable
fun MoodScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var todayMood by remember { mutableStateOf<MoodLevel?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Load on first composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            entries = Graph.mood.all()
            todayMood = Graph.mood.currentMood()
        }
        showPicker = todayMood == null
    }

    fun reload() {
        scope.launch(Dispatchers.IO) {
            entries = Graph.mood.all()
            todayMood = Graph.mood.currentMood()
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("😊 مزاجي اليومي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // ── Today card ───────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (todayMood != null) "مزاجك اليوم: ${todayMood!!.emoji} ${todayMood!!.labelAr}"
                        else "كيف حالك اليوم؟",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { showPicker = !showPicker }) {
                        Text(if (showPicker) "إغلاق" else "تغيير")
                    }
                }

                AnimatedVisibility(showPicker) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Mood buttons grid
                        val moods = MoodLevel.entries
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            moods.take(3).forEach { m -> MoodButton(m, todayMood == m) {
                                scope.launch(Dispatchers.IO) {
                                    Graph.mood.logToday(m, noteText)
                                    Graph.onMoodLogged(m)
                                }
                                todayMood = m; saved = true; showPicker = false; reload()
                            }}
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            moods.drop(3).forEach { m -> MoodButton(m, todayMood == m) {
                                scope.launch(Dispatchers.IO) {
                                    Graph.mood.logToday(m, noteText)
                                    Graph.onMoodLogged(m)
                                }
                                todayMood = m; saved = true; showPicker = false; reload()
                            }}
                        }
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("ملاحظة (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                if (saved) {
                    Text("✅ تم حفظ مزاجك!", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Weekly mini chart ────────────────────────────────────────────────
        val week = entries.takeLast(7)
        if (week.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("آخر ${week.size} أيام", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        week.forEach { e ->
                            MoodBar(e)
                        }
                    }
                }
            }
        }

        // ── History list ─────────────────────────────────────────────────────
        if (entries.isNotEmpty()) {
            Text("السجل", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { e ->
                    MoodHistoryCard(e)
                }
            }
        }
    }
}

@Composable
private fun MoodButton(mood: MoodLevel, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(mood.emoji, fontSize = 28.sp)
        Text(mood.labelAr, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MoodBar(entry: MoodEntry) {
    val fraction = (entry.mood.score + 1) / 6f   // 0.17 … 1.0
    val color = when (entry.mood) {
        MoodLevel.GREAT   -> MaterialTheme.colorScheme.primary
        MoodLevel.GOOD    -> MaterialTheme.colorScheme.tertiary
        MoodLevel.OKAY    -> MaterialTheme.colorScheme.secondary
        MoodLevel.TIRED   -> MaterialTheme.colorScheme.outlineVariant
        MoodLevel.STRESSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        MoodLevel.SAD     -> MaterialTheme.colorScheme.error
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(entry.mood.emoji, fontSize = 14.sp)
        Box(
            Modifier
                .width(28.dp)
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(color),
        )
        Text(entry.day.takeLast(5), style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MoodHistoryCard(entry: MoodEntry) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(entry.mood.emoji, fontSize = 32.sp)
            Column(Modifier.weight(1f)) {
                Text(entry.mood.labelAr, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(entry.day, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
