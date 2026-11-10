package com.mimo.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mimo.companion.R
import com.mimo.companion.domain.steps.StepState
import java.util.Locale

@Composable
fun StatsScreen(modifier: Modifier = Modifier, vm: StatsViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium)

        // ── Steps today ──────────────────────────────────────────────────────
        StatCard(title = stringResource(R.string.stats_steps_today)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatStepsStats(state.todaySteps),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (state.todaySteps >= state.stepGoal) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "/ ${formatStepsStats(state.stepGoal)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        // ── 7-day bar chart ──────────────────────────────────────────────────
        if (state.week.isNotEmpty()) {
            StatCard(title = stringResource(R.string.stats_week_title)) {
                Spacer(Modifier.height(4.dp))
                WeekChart(week = state.week, prayerEnabled = state.prayerEnabled)
            }
        }

        // ── Weekly totals ────────────────────────────────────────────────────
        val totalRemDone = state.week.sumOf { it.remindersCompleted }
        val totalPrayers = state.week.sumOf { it.prayersLogged }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = stringResource(R.string.stats_reminders_done),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "$totalRemDone",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.stats_this_week),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.prayerEnabled) {
                StatCard(
                    title = stringResource(R.string.stats_prayers_logged),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "$totalPrayers",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        stringResource(R.string.stats_this_week),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── WeekChart ────────────────────────────────────────────────────────────────

@Composable
private fun WeekChart(week: List<DayStats>, prayerEnabled: Boolean) {
    val maxRem = week.maxOf { it.remindersCompleted }.coerceAtLeast(1)
    val maxPrayer = if (prayerEnabled) 5 else 1

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Prayer dots (up to 5)
                if (prayerEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { i ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (i < day.prayersLogged) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                            )
                        }
                    }
                }

                // Reminders bar
                val barHeightFraction = day.remindersCompleted.toFloat() / maxRem
                val barHeight = (barHeightFraction * 80).coerceAtLeast(4f).dp
                Box(
                    Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (day.remindersCompleted > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )

                // Count
                Text(
                    if (day.remindersCompleted > 0) "${day.remindersCompleted}" else "–",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Day label
                Text(
                    day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendDot(color = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.stats_legend_reminders))
        if (prayerEnabled) LegendDot(color = MaterialTheme.colorScheme.secondary,
            label = stringResource(R.string.stats_legend_prayers))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── StatCard ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

private fun formatStepsStats(n: Int): String = String.format(Locale.US, "%,d", n)
