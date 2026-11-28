package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph

@Composable
fun WaterScreen(onBack: () -> Unit) {
    val glasses by Graph.water.glasses.collectAsState()
    val goal = Graph.water.dailyGoal
    val fraction = (glasses.toFloat() / goal).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("اشرب ماء 💧", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth().height(16.dp))
        Spacer(Modifier.height(8.dp))
        Text("$glasses / $goal كوب", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (Graph.water.goalReached()) {
            Text("🎉 أحسنت! وصلت لهدفك اليومي!", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Button(onClick = { Graph.water.drink(); Graph.onWaterDrunk(Graph.water.glasses.value, goal) }) {
                Text("+ كوب 💧", fontSize = 16.sp)
            }
            OutlinedButton(onClick = { Graph.water.undo() }) { Text("تراجع") }
        }
        Spacer(Modifier.height(24.dp))
        Text("الأكواب الموزعة:", fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier.fillMaxWidth()) {
            items(goal) { i ->
                Text(if (i < glasses) "💧" else "🫙", fontSize = 24.sp, modifier = Modifier.padding(4.dp))
            }
        }
    }
}
