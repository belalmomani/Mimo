package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimo.companion.Graph
import com.mimo.companion.sleep.SleepEntry

/**
 * "نومي" — sleep tracking screen.
 */
@Composable
fun SleepScreen(modifier: Modifier = Modifier) {
    val entries by Graph.sleep.entries.collectAsState()
    val bedH by Graph.sleep.bedtimeHourFlow.collectAsState()
    val bedM by Graph.sleep.bedtimeMinFlow.collectAsState()
    val wakeH by Graph.sleep.wakeHourFlow.collectAsState()
    val wakeM by Graph.sleep.wakeMinFlow.collectAsState()

    var logBedH by remember { mutableStateOf(bedH) }
    var logBedM by remember { mutableStateOf(bedM) }
    var logWakeH by remember { mutableStateOf(wakeH) }
    var logWakeM by remember { mutableStateOf(wakeM) }
    var saved by remember { mutableStateOf(false) }

    val week = Graph.sleep.lastDays(7)
    val avgHours = if (week.isEmpty()) 0f else week.map { it.durationHours }.average().toFloat()

    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("🌙 تتبع النوم", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // ── Stats row ────────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("متوسط النوم", "%.1f ساعة".format(avgHours), Modifier.weight(1f))
            StatCard("أيام مسجّلة", "${entries.size} يوم", Modifier.weight(1f))
        }

        // ── Log today ────────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("سجّل نومك اليوم", style = MaterialTheme.typography.titleMedium)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("وقت النوم", style = MaterialTheme.typography.labelMedium)
                        TimeRow(logBedH, logBedM,
                            onHour = { logBedH = it },
                            onMin  = { logBedM = it })
                    }
                    Text("→", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.weight(1f)) {
                        Text("وقت الاستيقاظ", style = MaterialTheme.typography.labelMedium)
                        TimeRow(logWakeH, logWakeM,
                            onHour = { logWakeH = it },
                            onMin  = { logWakeM = it })
                    }
                }

                val preview = SleepEntry("today", logBedH * 60 + logBedM, logWakeH * 60 + logWakeM)
                Text("المدة: ${preview.formatDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = {
                        Graph.sleep.logSleep(logBedH, logBedM, logWakeH, logWakeM)
                        Graph.onSleepLogged(preview.durationHours)
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("💾 حفظ") }

                if (saved) Text("✅ تم الحفظ!", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Reminder settings ────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⏰ تذكيرات النوم", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("موعد النوم", style = MaterialTheme.typography.labelSmall)
                        TimeRow(bedH, bedM,
                            onHour = { Graph.sleep.bedtimeHour = it },
                            onMin  = { Graph.sleep.bedtimeMin  = it })
                    }
                    Column(Modifier.weight(1f)) {
                        Text("موعد الاستيقاظ", style = MaterialTheme.typography.labelSmall)
                        TimeRow(wakeH, wakeM,
                            onHour = { Graph.sleep.wakeHour = it },
                            onMin  = { Graph.sleep.wakeMin  = it })
                    }
                }
                Text("ميمو سيذكّرك في هذه الأوقات", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── History ──────────────────────────────────────────────────────────
        if (entries.isNotEmpty()) {
            Text("السجل", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries.take(14)) { e -> SleepCard(e) }
            }
        }
    }
}

@Composable
private fun TimeRow(h: Int, m: Int, onHour: (Int) -> Unit, onMin: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NumberPicker(h, 0, 23, onHour, Modifier.weight(1f))
        Text(":", style = MaterialTheme.typography.titleMedium)
        NumberPicker(m, 0, 59, onMin, Modifier.weight(1f))
    }
}

@Composable
private fun NumberPicker(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = "%02d".format(value),
        onValueChange = { s -> s.toIntOrNull()?.coerceIn(min, max)?.let { onChange(it) } },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SleepCard(entry: SleepEntry) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("🌙 ${entry.date}", style = MaterialTheme.typography.bodyMedium)
            Text("${entry.formatBedtime()} → ${entry.formatWake()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entry.formatDuration(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
