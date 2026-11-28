package com.mimo.companion.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.quotes.Quote
import com.mimo.companion.quotes.QuoteBank

@Composable
fun QuoteScreen(onBack: () -> Unit) {
    var current by remember { mutableStateOf(QuoteBank.todayQuote()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
            Text("اقتباس اليوم ✨", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(40.dp))

        AnimatedContent(targetState = current) { quote ->
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❝", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
                    Text(quote.text, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 28.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("— ${quote.author}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { Graph.speakQuote(current) }) { Text("🔊 اقرأها") }
            OutlinedButton(onClick = { current = QuoteBank.randomQuote() }) { Text("🔀 اقتباس آخر") }
        }
    }
}
