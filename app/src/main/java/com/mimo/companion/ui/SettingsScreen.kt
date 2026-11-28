package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph

enum class SettingsDestination {
    NONE, WATER, POMODORO, MOOD, QUOTE, SLEEP, CUSTOM_PHRASES, STORE, POINTS_HISTORY
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var dest by remember { mutableStateOf(SettingsDestination.NONE) }

    when (dest) {
        SettingsDestination.WATER -> WaterScreen { dest = SettingsDestination.NONE }
        SettingsDestination.POMODORO -> PomodoroScreen { dest = SettingsDestination.NONE }
        SettingsDestination.MOOD -> MoodScreen { dest = SettingsDestination.NONE }
        SettingsDestination.QUOTE -> QuoteScreen { dest = SettingsDestination.NONE }
        SettingsDestination.SLEEP -> SleepScreen { dest = SettingsDestination.NONE }
        SettingsDestination.CUSTOM_PHRASES -> CustomPhrasesScreen { dest = SettingsDestination.NONE }
        SettingsDestination.STORE -> StoreScreen { dest = SettingsDestination.NONE }
        SettingsDestination.POINTS_HISTORY -> PointsHistoryScreen { dest = SettingsDestination.NONE }
        SettingsDestination.NONE -> SettingsMenu(onBack) { dest = it }
    }
}

@Composable
private fun SettingsMenu(onBack: () -> Unit, navigate: (SettingsDestination) -> Unit) {
    val balance by Graph.points.balance.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("الإعدادات", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))

        // Points banner
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text("رصيد نقاطك", fontSize = 13.sp)
                    Text("$balance نقطة ⭐", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                OutlinedButton(onClick = { navigate(SettingsDestination.POINTS_HISTORY) }) {
                    Text("السجل")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SettingsButton("🛍 متجر Mimo") { navigate(SettingsDestination.STORE) } }
            item { SettingsButton("💧 اشرب ماء") { navigate(SettingsDestination.WATER) } }
            item { SettingsButton("⏱ وضع التركيز") { navigate(SettingsDestination.POMODORO) } }
            item { SettingsButton("😊 مزاجي اليومي") { navigate(SettingsDestination.MOOD) } }
            item { SettingsButton("✨ اقتباس اليوم") { navigate(SettingsDestination.QUOTE) } }
            item { SettingsButton("🌙 تتبع النوم") { navigate(SettingsDestination.SLEEP) } }
            item { SettingsButton("💬 العبارات المخصصة") { navigate(SettingsDestination.CUSTOM_PHRASES) } }
        }
    }
}

@Composable
private fun SettingsButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(label, fontSize = 16.sp)
    }
}
