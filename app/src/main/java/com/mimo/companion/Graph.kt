package com.mimo.companion

import android.content.Context
import com.mimo.companion.ads.DisabledRewardedAdProvider
import com.mimo.companion.ads.RewardedAdProvider
import com.mimo.companion.ads.StartIoRewardedProvider
import com.mimo.companion.billing.BillingRepository
import com.mimo.companion.billing.NotConfiguredBillingRepository
import com.mimo.companion.data.db.MimoDatabase
import com.mimo.companion.data.db.RoomLedgerStorage
import com.mimo.companion.domain.DEFAULT_SKIN_ID
import com.mimo.companion.domain.PointsManager
import com.mimo.companion.domain.RewardConfig
import com.mimo.companion.domain.RewardedAdsManager
import com.mimo.companion.domain.SkinCatalog
import com.mimo.companion.domain.SkinUnlockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tiny manual DI container (no framework needed for a project this size). Created once in [MimoApp]. */
object Graph {
    lateinit var storage: RoomLedgerStorage; private set
    lateinit var points: PointsManager; private set
    lateinit var skins: SkinUnlockManager; private set
    lateinit var rewards: RewardedAdsManager; private set
    lateinit var adProvider: RewardedAdProvider; private set
    var billing: BillingRepository = NotConfiguredBillingRepository; private set

    /** Edit [RewardConfig] defaults (or pass values here) to change points per ad, daily limit, interval, welcome bonus... */
    val rewardConfig = RewardConfig()

    private val _activeSkin = MutableStateFlow(DEFAULT_SKIN_ID)
    /** The look currently worn by the floating companion; the overlay service observes it. */
    val activeSkin: StateFlow<String> get() = _activeSkin

    fun init(context: Context) {
        val db = MimoDatabase.build(context)
        storage = RoomLedgerStorage(db)
        points = PointsManager(storage)
        skins = SkinUnlockManager(storage, points, SkinCatalog.ALL)
        rewards = RewardedAdsManager(storage, points, skins, rewardConfig)
        adProvider = if (BuildConfig.ADS_ENABLED) StartIoRewardedProvider(context.applicationContext).also { it.init() } else DisabledRewardedAdProvider
    }

    /** Must be called off the main thread (reads the database). */
    fun loadActiveSkin() { _activeSkin.value = skins.activeSkinId() }
    fun publishActiveSkin(id: String) { _activeSkin.value = id }
}
