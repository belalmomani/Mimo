package com.mimo.companion.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimo.companion.R
import com.mimo.companion.face.MimoFace
import com.mimo.companion.overlay.FloatingService

@Composable
fun HomeScreen(state: StoreState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var running by remember { mutableStateOf(FloatingService.running) }
    var needPermission by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // The service works with or without the notification being visible; start either way.
        FloatingService.start(ctx); running = true
    }

    fun startOverlay() {
        if (!Settings.canDrawOverlays(ctx)) { needPermission = true; return }
        needPermission = false
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { FloatingService.start(ctx); running = true }
    }

    Column(
        modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyLarge)
        MimoFace(state.activeSkinId, Modifier.size(220.dp))
        Text(stringResource(R.string.points_balance, state.balance.toInt()), style = MaterialTheme.typography.titleMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_overlay_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_overlay_desc), style = MaterialTheme.typography.bodyMedium)
                if (needPermission) {
                    Text(stringResource(R.string.overlay_permission_needed), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                    }) { Text(stringResource(R.string.overlay_open_settings)) }
                }
                if (running) Button(onClick = { FloatingService.stop(ctx); running = false }) { Text(stringResource(R.string.overlay_stop)) }
                else Button(onClick = ::startOverlay) { Text(stringResource(R.string.overlay_start)) }
            }
        }
    }
}
