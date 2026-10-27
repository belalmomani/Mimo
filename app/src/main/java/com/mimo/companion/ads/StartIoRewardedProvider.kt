package com.mimo.companion.ads

import android.app.Activity
import android.content.Context
import com.mimo.companion.BuildConfig
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import java.util.UUID

/**
 * Start.io rewarded video, written against the API shown in the vendor's integration guide
 * and sample (StartAppAd + AdMode.REWARDED_VIDEO + VideoListener.onVideoCompleted + AdEventListener).
 *
 * IMPORTANT LIMIT: onVideoCompleted is a CLIENT-side signal. Start.io's public guide offers no
 * server-side reward verification for this API, so a rooted/modified device could fake it.
 * The ledger prevents double payment and enforces limits, but it cannot make a local balance tamper-proof.
 */
class StartIoRewardedProvider(private val appContext: Context) : RewardedAdProvider {
    override val name = "Start.io"

    @Volatile private var initialised = false
    override val isAvailable: Boolean get() = initialised

    /** Initialise once, from Application.onCreate (main thread). Failure leaves the provider unavailable, never crashes. */
    fun init() {
        if (initialised || !BuildConfig.ADS_ENABLED || BuildConfig.STARTIO_APP_ID.isBlank()) return
        initialised = try {
            // returnAds=false: no "return ads" outside the app, so nothing can pop over the floating companion.
            StartAppSDK.init(appContext, BuildConfig.STARTIO_APP_ID, false)
            true
        } catch (t: Throwable) {
            false
        }
    }

    override fun show(activity: Activity, callback: RewardedAdProvider.Callback) {
        if (!initialised) { callback.onUnavailable("Start.io not initialised"); return }
        val sessionId = UUID.randomUUID().toString()
        var rewarded = false
        var finished = false
        val ad = StartAppAd(activity)
        ad.setVideoListener(VideoListener {
            // Completion signal. Guard so one session can never report twice.
            if (!rewarded) { rewarded = true; finished = true; callback.onRewardEarned(sessionId) }
        })
        val loaded = try {
            ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
                override fun onReceiveAd(p0: Ad) {
                    try {
                        if (!ad.showAd()) callback.onUnavailable("showAd returned false")
                    } catch (t: Throwable) {
                        callback.onUnavailable(t.message ?: "show failed")
                    }
                }
                override fun onFailedToReceiveAd(p0: Ad?) {
                    if (!finished) { finished = true; callback.onUnavailable("no rewarded ad received") }
                }
            })
        } catch (t: Throwable) {
            callback.onUnavailable(t.message ?: "load failed"); return
        }
        if (!loaded) { if (!finished) { finished = true; callback.onUnavailable("loadAd rejected") } }
    }
}
