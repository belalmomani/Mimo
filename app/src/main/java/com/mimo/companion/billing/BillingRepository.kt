package com.mimo.companion.billing

import com.mimo.companion.domain.PointsPack

/**
 * Real-money point packs. NOT connected on purpose: there is no Play Console app, no product ids
 * and no tested purchase flow yet, and this project refuses to fake a purchase.
 *
 * When ready: add `com.android.billingclient:billing` (8.x or newer - Google requires 8+ for new
 * apps/updates from 2026-08-31), create consumable products in Play Console, put their ids in
 * PointsPacks.ALL[*].playProductId, implement this interface with BillingClient, and on each
 * VERIFIED + acknowledged/consumed purchase call
 * `PointsManager.credit(pack.points, POINTS_PURCHASE, txId = PointsManager.purchaseTxId(purchaseToken))`
 * (the fixed tx id makes replays/restores idempotent). Because real money is involved, verify
 * purchases on a server (Play Developer API) before crediting - a local balance alone is not trustworthy.
 */
interface BillingRepository {
    val state: BillingState
    suspend fun productsFor(packs: List<PointsPack>): List<PackOffer>
    /** @return the purchase token once Google confirms the purchase, null if cancelled/pending/failed. */
    suspend fun purchase(activity: android.app.Activity, pack: PointsPack): PurchaseResult
    suspend fun restore(): List<String>
}

enum class BillingState { NOT_CONFIGURED, READY, ERROR }
data class PackOffer(val pack: PointsPack, val formattedPrice: String)
sealed interface PurchaseResult {
    data class Purchased(val purchaseToken: String) : PurchaseResult
    object Cancelled : PurchaseResult
    object Pending : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

object NotConfiguredBillingRepository : BillingRepository {
    override val state = BillingState.NOT_CONFIGURED
    override suspend fun productsFor(packs: List<PointsPack>) = emptyList<PackOffer>()
    override suspend fun purchase(activity: android.app.Activity, pack: PointsPack) = PurchaseResult.Error("billing not configured")
    override suspend fun restore() = emptyList<String>()
}
