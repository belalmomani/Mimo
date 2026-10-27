package com.mimo.companion.ui

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimo.companion.Graph
import com.mimo.companion.R
import com.mimo.companion.ads.RewardedAdProvider
import com.mimo.companion.domain.AdProgressResult
import com.mimo.companion.domain.RewardOutcome
import com.mimo.companion.domain.RewardRejection
import com.mimo.companion.domain.SkinDef
import com.mimo.companion.domain.SkinStatus
import com.mimo.companion.domain.UnlockResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList())

data class SkinItem(val def: SkinDef, val status: SkinStatus, val adsDone: Int)

data class StoreState(
    val balance: Long = 0,
    val items: List<SkinItem> = emptyList(),
    val activeSkinId: String = "default",
    val adBusy: Boolean = false,
)

class StoreViewModel : ViewModel() {
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = Graph.skins.catalog.map { SkinItem(it, Graph.skins.status(it), Graph.storage.adProgress(it.id)) }
            _state.update { it.copy(balance = Graph.points.balance(), items = items, activeSkinId = Graph.skins.activeSkinId()) }
        }
    }

    private fun say(res: Int, vararg args: Any) { _messages.tryEmit(UiMessage(res, args.toList())) }

    // ---- earn points -------------------------------------------------------------------------------------
    fun earnPoints(activity: Activity) {
        if (_state.value.adBusy) return
        viewModelScope.launch(Dispatchers.IO) {
            val avail = Graph.rewards.earnAvailability()
            if (!avail.allowed) { explain(avail.reason, avail.retryAfterSeconds); return@launch }
            showAd(activity) { sessionId ->
                when (val r = Graph.rewards.onPointsAdCompleted(sessionId)) {
                    is RewardOutcome.PointsGranted -> say(R.string.msg_points_earned, r.points)
                    is RewardOutcome.Rejected -> explain(r.reason, r.retryAfterSeconds)
                    is RewardOutcome.Failed -> say(R.string.msg_error)
                    is RewardOutcome.SkinProgress -> Unit
                }
            }
        }
    }

    // ---- unlock progress by ads ------------------------------------------------------------------------------
    fun watchAdForSkin(activity: Activity, skinId: String) {
        if (_state.value.adBusy) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!Graph.rewardConfig.rewardedAdsEnabled) { say(R.string.msg_ads_disabled); return@launch }
            showAd(activity) { sessionId ->
                when (val r = Graph.rewards.onSkinAdCompleted(sessionId, skinId)) {
                    is RewardOutcome.SkinProgress -> when (val p = r.result) {
                        is AdProgressResult.Unlocked -> say(R.string.msg_unlocked)
                        is AdProgressResult.Progress -> say(R.string.msg_progress, p.done, p.total)
                        AdProgressResult.AlreadyOwned -> say(R.string.msg_already_owned)
                        AdProgressResult.NotUnlockableWithAds -> say(R.string.msg_error)
                    }
                    is RewardOutcome.Rejected -> explain(r.reason, r.retryAfterSeconds)
                    is RewardOutcome.Failed -> say(R.string.msg_error)
                    is RewardOutcome.PointsGranted -> Unit
                }
            }
        }
    }

    /** Shows one rewarded ad on the main thread; [onReward] runs on IO only after a verified completion. */
    private fun showAd(activity: Activity, onReward: suspend (String) -> Unit) {
        _state.update { it.copy(adBusy = true) }
        val cb = object : RewardedAdProvider.Callback {
            override fun onRewardEarned(sessionId: String) {
                viewModelScope.launch(Dispatchers.IO) { onReward(sessionId); finishAd() }
            }
            override fun onUnavailable(reason: String) { say(R.string.msg_ad_not_ready); _state.update { it.copy(adBusy = false) } }
            override fun onClosedWithoutReward() { say(R.string.msg_ad_incomplete); _state.update { it.copy(adBusy = false) } }
        }
        activity.runOnUiThread {
            try { Graph.adProvider.show(activity, cb) }
            catch (t: Throwable) { say(R.string.msg_ad_failed); _state.update { it.copy(adBusy = false) } }
        }
    }

    private fun finishAd() { _state.update { it.copy(adBusy = false) }; refresh() }

    private fun explain(reason: RewardRejection?, retry: Long) = when (reason) {
        RewardRejection.DISABLED -> say(R.string.msg_ads_disabled)
        RewardRejection.DAILY_LIMIT -> say(R.string.msg_daily_limit)
        RewardRejection.TOO_SOON -> say(R.string.msg_too_soon, retry.toInt())
        RewardRejection.DUPLICATE_EVENT, RewardRejection.NOT_APPLICABLE, null -> Unit
    }

    // ---- points unlock / selection --------------------------------------------------------------------------------
    fun unlockWithPoints(skinId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val r = Graph.skins.unlockWithPoints(skinId)) {
                is UnlockResult.Unlocked -> say(R.string.msg_unlocked)
                is UnlockResult.InsufficientPoints -> say(R.string.msg_not_enough, r.available.toInt(), r.required.toInt())
                UnlockResult.AlreadyOwned -> say(R.string.msg_already_owned)
                UnlockResult.NotUnlockableWithPoints, UnlockResult.UnknownSkin -> say(R.string.msg_error)
                is UnlockResult.Failed -> say(R.string.msg_error)
            }
            refresh(); onDone()
        }
    }

    fun activate(skinId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (Graph.skins.activate(skinId)) Graph.publishActiveSkin(skinId)
            refresh()
        }
    }
}
