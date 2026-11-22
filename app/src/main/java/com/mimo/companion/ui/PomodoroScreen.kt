package com.mimo.companion.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.focus.PomodoroPhase
import com.mimo.companion.focus.PomodoroState

/**
 * "وضع التركيز" – Pomodoro timer screen embedded as a Dialog from SettingsScreen.
 */
@Composable
fun PomodoroScreen(modifier: Modifier = Modifier) {
    val state by Graph.pomodoro.state.collectAsState()

    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            "⏱ وضع التركيز",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // ── Circular timer ───────────────────────────────────────────────────
        PomodoroCircle(state)

        // ── Phase label ──────────────────────────────────────────────────────
        val phaseLabel = when (state.phase) {
            PomodoroPhase.IDLE  -> "جاهز للبدء"
            PomodoroPhase.WORK  -> "🎯 وقت التركيز — الجولة ${state.round}"
            PomodoroPhase.BREAK -> "☕ وقت الراحة"
        }
        Text(phaseLabel, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // ── Controls ─────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.phase == PomodoroPhase.IDLE) {
                Button(onClick = { Graph.pomodoro.start() }, modifier = Modifier.weight(1f)) {
                    Text("ابدأ")
                }
            } else {
                OutlinedButton(onClick = { Graph.pomodoro.stop() }, modifier = Modifier.weight(1f)) {
                    Text("إيقاف")
                }
            }
        }

        // ── Info row ─────────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatChip("⚡ عمل", "25 دقيقة")
                StatChip("☕ راحة", "5 دقائق")
            }
        }
    }
}

@Composable
private fun PomodoroCircle(state: PomodoroState) {
    val totalSeconds = when (state.phase) {
        PomodoroPhase.WORK  -> 25 * 60f
        PomodoroPhase.BREAK -> 5 * 60f
        PomodoroPhase.IDLE  -> 25 * 60f
    }
    val progress = if (state.phase == PomodoroPhase.IDLE) 1f
                   else state.secondsLeft / totalSeconds

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "pomodoroArc",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = when (state.phase) {
        PomodoroPhase.WORK  -> MaterialTheme.colorScheme.primary
        PomodoroPhase.BREAK -> MaterialTheme.colorScheme.tertiary
        PomodoroPhase.IDLE  -> MaterialTheme.colorScheme.outlineVariant
    }

    val mm = state.secondsLeft / 60
    val ss = state.secondsLeft % 60
    val timeText = if (state.phase == PomodoroPhase.IDLE) "25:00"
                   else "%02d:%02d".format(mm, ss)

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(180.dp)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            val inset = 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            // Track
            drawArc(trackColor, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            // Progress arc
            drawArc(arcColor, startAngle = -90f, sweepAngle = 360f * animatedProgress,
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        }
        Text(timeText, fontSize = 36.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
