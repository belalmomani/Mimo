package com.mimo.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val overlayPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkAndStart()
        }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        requestNotificationPermission()

        setContent {
            MaterialTheme {
                MimoHome(
                    onStartService = { checkAndStart() },
                    onStopService = { stopService(Intent(this, FloatingService::class.java)) }
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermission.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        } else {
            startForegroundService(Intent(this, FloatingService::class.java))
        }
    }
}

@Composable
private fun MimoHome(onStartService: () -> Unit, onStopService: () -> Unit) {
    var showSettings by remember { mutableStateOf(false) }
    val balance by Graph.points.balance.collectAsState()

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Mimo", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Your Floating Digital Companion", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "$balance نقطة ⭐",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onStartService, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("▶ تشغيل Mimo", fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStopService, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("⏹ إيقاف Mimo", fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("⚙️ الإعدادات والمتجر", fontSize = 16.sp)
            }
        }
    }
}
