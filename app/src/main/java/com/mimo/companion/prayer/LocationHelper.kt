package com.mimo.companion.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * One-shot coarse location read — enough to pick the nearest city.
 *
 * Uses [LocationManager.getCurrentLocation] (API 30+; minSdk is 26, so we
 * fall back to [LocationManager.getLastKnownLocation] on API 26-29).
 *
 * The caller never holds a live listener; the fix is used once and discarded.
 * No network/fused provider is required — NETWORK_PROVIDER (coarse, permission:
 * ACCESS_COARSE_LOCATION) is tried first, then GPS_PROVIDER as fallback.
 */
object LocationHelper {

    sealed interface Result {
        data class Ok(val lat: Double, val lng: Double) : Result
        /** The permission ACCESS_COARSE_LOCATION was not granted. */
        object NoPermission : Result
        /** Could not get a fix (providers disabled, old device, or timeout). */
        object Unavailable : Result
    }

    private const val TIMEOUT_MS = 8_000L

    /**
     * Obtains a location fix and delivers the result on the **main thread** via [onResult].
     * Must be called from any thread (uses its own handler for the timeout).
     */
    fun get(context: Context, onResult: (Result) -> Unit) {
        val ctx = context.applicationContext

        // 1. Permission check
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            deliver(onResult, Result.NoPermission); return
        }

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val mainHandler = Handler(Looper.getMainLooper())

        // 2. Try fast last-known first (instant, no battery cost)
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { lm.isProviderEnabled(it) }

        val lastKnown: Location? = providers
            .mapNotNull { runCatching { @Suppress("DEPRECATION") lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (lastKnown != null) {
            deliver(onResult, Result.Ok(lastKnown.latitude, lastKnown.longitude)); return
        }

        // 3. No cached fix — request a fresh one (API 30+ path)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val provider = providers.firstOrNull() ?: run {
                deliver(onResult, Result.Unavailable); return
            }
            var done = false
            val signal = CancellationSignal()
            val timeout = mainHandler.postDelayed({
                if (!done) { done = true; signal.cancel(); deliver(onResult, Result.Unavailable) }
            }, TIMEOUT_MS)

            lm.getCurrentLocation(
                provider, signal,
                ctx.mainExecutor,
            ) { loc ->
                mainHandler.removeCallbacksAndMessages(timeout)
                if (!done) {
                    done = true
                    deliver(onResult, if (loc != null) Result.Ok(loc.latitude, loc.longitude) else Result.Unavailable)
                }
            }
        } else {
            // API 26-29: getCurrentLocation doesn't exist; no cached fix means unavailable for our purposes
            deliver(onResult, Result.Unavailable)
        }
    }

    private fun deliver(onResult: (Result) -> Unit, result: Result) {
        if (Looper.myLooper() == Looper.getMainLooper()) onResult(result)
        else Handler(Looper.getMainLooper()).post { onResult(result) }
    }
}
