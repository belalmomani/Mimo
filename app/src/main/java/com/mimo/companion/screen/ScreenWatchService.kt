package com.mimo.companion.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.mimo.companion.Graph
import com.mimo.companion.MainActivity
import com.mimo.companion.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * "ميمو يرى شاشتك" – MediaProjection foreground service.
 *
 * Captures the screen every [CAPTURE_INTERVAL_MS] milliseconds and passes the
 * compressed JPEG to [ScreenAnalyzer.analyze], which returns a short comment
 * that [Graph.presentCustom]-equivalent logic then shows to the user.
 *
 * Lifecycle:
 *  1. User taps "ميمو يرى شاشتك" toggle in Settings.
 *  2. MainActivity launches the MediaProjection permission dialog.
 *  3. On grant: starts this service with the resultCode + data intent.
 *  4. User taps toggle again (or the notification) → ACTION_STOP.
 *
 * Privacy note: screenshots never leave the device; they are analysed in
 * [ScreenAnalyzer] locally (rule-based) and immediately discarded.
 */
class ScreenWatchService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            running = false
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode == -1 || data == null) { stopSelf(); return START_NOT_STICKY }

        startForeground()
        startCapture(resultCode, data)
        running = true
        return START_NOT_STICKY
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private fun startForeground() {
        val channelId = "screen_watch"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "ميمو يرى شاشتك", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 10,
            Intent(this, ScreenWatchService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            this, 11,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_mimo)
            .setContentTitle("ميمو يراقب شاشتك")
            .setContentText("اضغط لإيقاف رؤية الشاشة")
            .setContentIntent(open)
            .addAction(0, "إيقاف", stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(2, n)
        }
    }

    // ── Capture loop ──────────────────────────────────────────────────────────

    private fun startCapture(resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)

        // Capture at 1/3 resolution to keep memory low
        val w = dm.widthPixels / 3
        val h = dm.heightPixels / 3
        val density = dm.densityDpi

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection!!.createVirtualDisplay(
            "mimo_screen", w, h, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null,
        )

        captureJob = scope.launch {
            delay(3_000L) // let display settle
            while (isActive) {
                captureAndAnalyze()
                delay(CAPTURE_INTERVAL_MS)
            }
        }
    }

    private fun captureAndAnalyze() {
        val reader = imageReader ?: return
        val image: Image = reader.acquireLatestImage() ?: return
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bmp = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888,
            )
            bmp.copyPixelsFromBuffer(buffer)

            // Compress to JPEG for analysis
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            bmp.recycle()

            val jpeg = baos.toByteArray()
            val comment = ScreenAnalyzer.analyze(jpeg, getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            if (comment != null && Graph.settings.value.voiceEnabled) {
                Graph.showScreenComment(comment)
            }
        } finally {
            image.close()
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        virtualDisplay = null
        imageReader = null
        projection = null
    }

    override fun onDestroy() {
        running = false
        stopCapture()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP      = "com.mimo.companion.STOP_SCREEN_WATCH"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA        = "data"
        /** Capture and analyse every 30 seconds while the feature is active. */
        private const val CAPTURE_INTERVAL_MS = 30_000L

        @Volatile var running = false; private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val i = Intent(context, ScreenWatchService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScreenWatchService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
