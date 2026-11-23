package com.mimo.companion.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.quotes.Quote
import com.mimo.companion.quotes.QuoteBank

/**
 * "اقتباس اليوم" — daily motivational quote screen.
 */
@Composable
fun QuoteScreen(modifier: Modifier = Modifier) {
    var quote by remember { mutableStateOf(QuoteBank.todayQuote()) }
    var isToday by remember { mutableStateOf(true) }

    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("✨ اقتباس اليوم", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)

        // ── Quote card ───────────────────────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "❝",
                    fontSize = 40.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                AnimatedContent(
                    targetState = quote,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "quoteAnim",
                ) { q ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            q.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (q.author.isNotBlank()) {
                            Text(
                                "— ${q.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
                if (isToday) {
                    AssistChip(
                        onClick = {},
                        label = { Text("اقتباس اليوم") },
                        leadingIcon = { Text("📅") },
                    )
                }
            }
        }

        // ── Action buttons ───────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Read aloud
            Button(
                onClick = { Graph.speakQuote(quote) },
                modifier = Modifier.weight(1f),
            ) { Text("🔊 اقرأها") }

            // Shuffle
            OutlinedButton(
                onClick = {
                    quote = QuoteBank.randomQuote()
                    isToday = false
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("اقتباس آخر")
            }
        }

        // ── Back to today ────────────────────────────────────────────────────
        if (!isToday) {
            TextButton(onClick = { quote = QuoteBank.todayQuote(); isToday = true }) {
                Text("العودة لاقتباس اليوم")
            }
        }

        // ── Quote count ──────────────────────────────────────────────────────
        Text(
            "${QuoteBank.quotes.size} اقتباس في المكتبة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
