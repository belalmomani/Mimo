package com.mimo.companion.ads

import android.app.Activity

/**
 * What the app needs from a rewarded-ad network. The store never touches an SDK directly:
 * swapping Start.io for another provider means writing one more implementation of this interface.
 *
 * Contract: [onRewardEarned] is called AT MOST ONCE per [show], and ONLY when the provider's own
 * completion signal says the reward conditions were met. Closing early / failing never calls it.
 */
interface RewardedAdProvider {
    val name: String

    /** True when the SDK is initialised and the provider can be asked to show an ad. */
    val isAvailable: Boolean

    fun show(activity: Activity, callback: Callback)

    interface Callback {
        /** The ad session finished with a valid reward. [sessionId] is unique per show() call. */
        fun onRewardEarned(sessionId: String)
        /** No ad could be loaded (no fill / offline / SDK error). */
        fun onUnavailable(reason: String)
        /** The user left before completion (or the ad ended without a reward signal). */
        fun onClosedWithoutReward()
    }
}

/** Used when ads are switched off (ADS_ENABLED=false) or the SDK failed to initialise. */
object DisabledRewardedAdProvider : RewardedAdProvider {
    override val name = "disabled"
    override val isAvailable = false
    override fun show(activity: Activity, callback: RewardedAdProvider.Callback) = callback.onUnavailable("ads disabled")
}
