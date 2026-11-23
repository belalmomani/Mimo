package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph

/**
 * "اشرب ماء" — daily water intake tracker.
 */
@Composable
fun WaterScreen(modifier: Modifier = Modifier) {
    val glasses by Graph.water.glasses.collectAsState()
    val goal = Graph.water.dailyGoal
    val progress = (glasses / goal.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("💧 اشرب ماء", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // ── Progress circle ──────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$glasses", fontSize = 42.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("/ $goal كوب", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Status message ───────────────────────────────────────────────────
        val msg = when {
            glasses == 0          -> "ابدأ يومك بكوب ماء 💧"
            glasses >= goal       -> "أحسنت! وصلت لهدفك اليوم 🎉"
            glasses >= goal * 0.5 -> "في المنتصف، استمر! 💪"
            else                  -> "كمّل، ${goal - glasses} كوب باقي"
        }
        Text(msg, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // ── Buttons ──────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = { Graph.water.undo() },
                enabled = glasses > 0,
                modifier = Modifier.weight(1f),
            ) { Text("↩ تراجع") }

            Button(
                onClick = {
                    Graph.water.drink()
                    Graph.onWaterDrunk(glasses + 1, goal)
                },
                modifier = Modifier.weight(1f),
                enabled = glasses < goal * 2,
            ) { Text("💧 شربت كوب") }
        }

        // ── Goal slider ──────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الهدف اليومي: $goal كوب", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { Graph.water.dailyGoal = it.toInt() },
                    valueRange = 4f..16f,
                    steps = 11,
                )
            }
        }

        // ── Glass indicators ─────────────────────────────────────────────────
        val rows = (goal + 3) / 4
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { col ->
                        val idx = row * 4 + col
                        if (idx < goal) {
                            val filled = idx < glasses
                            Surface(
                                shape = CircleShape,
                                color = if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (filled) "💧" else "○", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
