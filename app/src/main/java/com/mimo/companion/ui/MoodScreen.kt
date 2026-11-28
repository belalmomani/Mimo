package com.mimo.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.domain.mood.MoodLevel

@Composable
fun MoodScreen(onBack: () -> Unit) {
    val today = remember { java.time.LocalDate.now().toString() }
    var todayMood by remember { mutableStateOf(Graph.mood.currentMood()) }
    var note by remember { mutableStateOf("") }
    val history = remember { Graph.mood.lastDays(7) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Spacer(Modifier.width(8.dp))
            Text("مزاجي اليومي", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("كيف حالك اليوم؟", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                // Mood grid
                val moods = MoodLevel.values().toList()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in moods.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { level ->
                                MoodChip(
                                    level = level,
                                    selected = todayMood == level,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        Graph.mood.logToday(level, note)
                                        todayMood = level
                                        Graph.onMoodLogged(level)
                                    }
                                )
                            }
                            if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة اختيارية") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
            item {
                Text("آخر 7 أيام", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            items(history) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.day, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${entry.mood.emoji} ${entry.mood.labelAr}", fontSize = 14.sp)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun MoodChip(level: MoodLevel, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (selected) 8.dp else 2.dp,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(level.emoji, fontSize = 28.sp)
            Text(level.labelAr, fontSize = 11.sp)
        }
    }
}
