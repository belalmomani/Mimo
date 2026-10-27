package com.mimo.companion.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mimo.companion.Graph
import com.mimo.companion.MainActivity
import com.mimo.companion.R
import com.mimo.companion.face.MimoFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The floating companion: a transparent TYPE_APPLICATION_OVERLAY window hosting the Compose face.
 * Needs the user to grant "Display over other apps"; never started without it.
 * Runs as a foreground service (specialUse) so Android keeps it alive; it stops itself if the
 * permission is revoked, and is NOT restarted after a reboot (the user starts it from the app).
 *
 * Touch: drag moves it (position is saved), a short tap opens Mimo. The notification has a "Hide" action.
 */
class FloatingService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var wm: WindowManager
    private lateinit var prefs: OverlayPrefs
    private var view: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = OverlayPrefs(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startInForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (view == null) attachOverlay()
        running = true
        return START_NOT_STICKY   // do not pretend to survive process death: the user restarts it from the app
    }

    private fun startInForeground() {
        val channelId = "floating"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId, getString(R.string.overlay_notif_channel), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val hide = PendingIntent.getService(this, 1, Intent(this, FloatingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_mimo)
            .setContentTitle(getString(R.string.overlay_notif))
            .setContentIntent(open)
            .addAction(0, getString(R.string.overlay_notif_hide), hide)
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
    }

    private fun attachOverlay() {
        val density = resources.displayMetrics.density
        val sizePx = (prefs.sizeDp * density).toInt()
        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE: never steals keyboard focus; NOT_TOUCH_MODAL: touches outside pass through to the app below.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = prefs.x; y = prefs.y; alpha = prefs.alpha }

        val cv = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val skin by Graph.activeSkin.collectAsState()
                MimoFace(skinId = skin)
            }
            setOnTouchListener(dragListener())
        }
        view = cv
        wm.addView(cv, params)
        // make sure the active look is current even if the app process was just (re)created
        lifecycleScope.launch(Dispatchers.IO) { Graph.loadActiveSkin() }
    }

    private fun dragListener() = object : View.OnTouchListener {
        private var startX = 0; private var startY = 0; private var touchX = 0f; private var touchY = 0f; private var moved = false
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val slop = ViewConfiguration.get(this@FloatingService).scaledTouchSlop
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; touchX = e.rawX; touchY = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - touchX; val dy = e.rawY - touchY
                    if (moved || abs(dx) > slop || abs(dy) > slop) {
                        moved = true
                        params.x = (startX + dx).toInt(); params.y = (startY + dy).toInt()
                        view?.let { wm.updateViewLayout(it, params) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) { prefs.x = params.x; prefs.y = params.y }
                    else startActivity(Intent(this@FloatingService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            return true
        }
    }

    override fun onDestroy() {
        running = false
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }

    companion object {
        const val ACTION_STOP = "com.mimo.companion.STOP_OVERLAY"
        @Volatile var running = false; private set

        fun start(context: Context) {
            val i = Intent(context, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) { context.startService(Intent(context, FloatingService::class.java).setAction(ACTION_STOP)) }
    }
}
