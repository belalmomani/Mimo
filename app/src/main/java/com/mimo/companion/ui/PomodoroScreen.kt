package com.mimo.companion.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.focus.PomodoroPhase

@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val state by Graph.pomodoro.state.collectAsState()
    val phaseLabel = when (state.phase) {
        PomodoroPhase.IDLE -> "جاهز"
        PomodoroPhase.WORK -> "وقت التركيز 💪"
        PomodoroPhase.BREAK -> "وقت الراحة 🌿"
    }
    val totalSecs = when (state.phase) {
        PomodoroPhase.WORK -> 25 * 60f; PomodoroPhase.BREAK -> 5 * 60f; else -> 25 * 60f
    }
    val progress by animateFloatAsState(if (totalSecs > 0) state.secondsLeft / totalSecs else 1f)
    val colorScheme = MaterialTheme.colorScheme
    val minutes = state.secondsLeft / 60
    val seconds = state.secondsLeft % 60

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("وضع التركيز ⏱", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(phaseLabel, fontSize = 18.sp, color = colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        if (state.round > 0) Text("الجولة ${state.round}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                drawArc(color = colorScheme.surfaceVariant, startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawArc(color = colorScheme.primary, startAngle = -90f, sweepAngle = 360f * progress,
                    useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    fontSize = 42.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.phase == PomodoroPhase.IDLE) {
                Button(onClick = { Graph.pomodoro.start() }, modifier = Modifier.weight(1f)) {
                    Text("▶ ابدأ", fontSize = 16.sp)
                }
            } else {
                OutlinedButton(onClick = { Graph.pomodoro.stop() }, modifier = Modifier.weight(1f)) {
                    Text("⏹ إيقاف")
                }
            }
        }
    }
}
