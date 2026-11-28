package com.mimo.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.points.PointsTransaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PointsHistoryScreen(onBack: () -> Unit) {
    val balance by Graph.points.balance.collectAsState()
    val transactions by Graph.points.transactions.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("سجل النقاط", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Text("$balance ⭐", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions.reversed()) { tx ->
                TransactionRow(tx, dateFormat)
            }
            if (transactions.isEmpty()) {
                item { Text("لا توجد معاملات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: PointsTransaction, fmt: SimpleDateFormat) {
    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.type.labelAr, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (tx.note.isNotBlank()) Text(tx.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmt.format(Date(tx.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (tx.amount >= 0) "+${tx.amount} ⭐" else "${tx.amount} ⭐",
                color = if (tx.amount >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
