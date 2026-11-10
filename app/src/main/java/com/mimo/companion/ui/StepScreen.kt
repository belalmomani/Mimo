package com.mimo.companion.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph
import com.mimo.companion.R
import com.mimo.companion.domain.steps.StepState
import java.util.Locale

/** Shows today's step count, a circular progress ring, and a goal picker. */
@Composable
fun StepScreen(modifier: Modifier = Modifier) {
    val s by Graph.steps.state.collectAsState()

    // Permission launcher (Android 10+ only)
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { Graph.steps.onPermissionResult() }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.steps_title), style = MaterialTheme.typography.headlineSmall)

        when {
            // ── sensor absent ────────────────────────────────────────────
            !s.available && !s.permissionNeeded -> {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.steps_unavailable),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // ── permission needed (API 29+) ──────────────────────────────
            s.permissionNeeded -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.steps_perm_rationale), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            permLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }) { Text(stringResource(R.string.steps_grant_perm)) }
                    }
                }
            }

            // ── normal / counting ────────────────────────────────────────
            else -> {
                StepRingCard(s)
                GoalCard(s)
                Text(stringResource(R.string.steps_privacy), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StepRingCard(s: StepState) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { s.progress },
                    modifier = Modifier.size(140.dp),
                    strokeWidth = 10.dp,
                    color = if (s.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatSteps(s.todaySteps),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                    )
                    Text(stringResource(R.string.steps_label), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.steps_of_goal, formatSteps(s.goal)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (s.done) Text(
                stringResource(R.string.steps_goal_done),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GoalCard(s: StepState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.steps_goal_title), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = { Graph.steps.setGoal(s.goal - StepState.GOAL_STEP) },
                    enabled = s.goal > StepState.MIN_GOAL,
                ) { Text("−") }
                Text(
                    formatSteps(s.goal),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                TextButton(
                    onClick = { Graph.steps.setGoal(s.goal + StepState.GOAL_STEP) },
                    enabled = s.goal < StepState.MAX_GOAL,
                ) { Text("+") }
            }
        }
    }
}

/** Formats a step count with thousands separator (e.g. 6000 → "6,000"). */
private fun formatSteps(n: Int): String = String.format(Locale.US, "%,d", n)
