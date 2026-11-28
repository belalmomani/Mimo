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
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph

@Composable
fun SleepScreen(onBack: () -> Unit) {
    val tracker = Graph.sleep
    var bedH by remember { mutableIntStateOf(tracker.bedtimeHour) }
    var bedM by remember { mutableIntStateOf(tracker.bedtimeMin) }
    var wakeH by remember { mutableIntStateOf(tracker.wakeHour) }
    var wakeM by remember { mutableIntStateOf(tracker.wakeMin) }
    val history by tracker.entries.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("تتبع النوم 🌙", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("وقت النوم", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker("ساعة", bedH, 0, 23) { bedH = it }
                    Text(":", fontSize = 20.sp)
                    NumberPicker("دقيقة", bedM, 0, 59) { bedM = it }
                }
            }
            item {
                Text("وقت الاستيقاظ", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberPicker("ساعة", wakeH, 0, 23) { wakeH = it }
                    Text(":", fontSize = 20.sp)
                    NumberPicker("دقيقة", wakeM, 0, 59) { wakeM = it }
                }
            }
            item {
                Button(
                    onClick = {
                        tracker.bedtimeHour = bedH; tracker.bedtimeMin = bedM
                        tracker.wakeHour = wakeH; tracker.wakeMin = wakeM
                        tracker.logSleep(bedH, bedM, wakeH, wakeM)
                        val entry = tracker.todayEntry()
                        if (entry != null) Graph.onSleepLogged(entry.durationMinutes / 60)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("حفظ وتسجيل النوم") }
            }
            item {
                Divider()
                Text("السجل", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(history.reversed()) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.date, fontSize = 13.sp)
                    val h = entry.durationMinutes / 60
                    val m = entry.durationMinutes % 60
                    Text("${h}س ${m}د", fontSize = 13.sp)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun NumberPicker(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onValueChange(value - 1) }) { Text("−") }
            Text("%02d".format(value), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { if (value < max) onValueChange(value + 1) }) { Text("+") }
        }
    }
}
