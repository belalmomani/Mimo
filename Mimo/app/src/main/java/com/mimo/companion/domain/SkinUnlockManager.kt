package com.mimo.companion.domain

/**
 * Ownership, unlocking and the active look. Unlocking with points debits the balance and grants
 * ownership in ONE storage transaction, so a failure can never charge without delivering (or the reverse).
 */
class SkinUnlockManager(
    private val storage: LedgerStorage,
    private val points: PointsManager,
    val catalog: List<SkinDef>,
    private val time: TimeSource = SystemTimeSource,
) {
    fun skin(id: String): SkinDef? = catalog.firstOrNull { it.id == id }

    /** Free looks and the default look are always usable; everything else needs ownership. */
    fun isUsable(skinId: String): Boolean {
        if (skinId == DEFAULT_SKIN_ID) return true
        val skin = skin(skinId) ?: return false
        return skin.isFree || storage.isOwned(skin.id)
    }

    fun status(skin: SkinDef): SkinStatus {
        if (skin.isFree) return SkinStatus.Free
        if (storage.isOwned(skin.id)) return SkinStatus.Owned
        val done = storage.adProgress(skin.id)
        return if (skin.allowsAds && done > 0) SkinStatus.InProgress(done.coerceAtMost(skin.adsRequired), skin.adsRequired) else SkinStatus.Locked
    }

    /** Skins the user can put on right now (free + owned), in catalog order. */
    fun usableSkins(): List<SkinDef> = catalog.filter { isUsable(it.id) }

    fun unlockWithPoints(skinId: String): UnlockResult {
        val skin = skin(skinId) ?: return UnlockResult.UnknownSkin
        return try {
            storage.inTransaction {
                // Checked inside the transaction so a double tap cannot slip past between check and charge.
                if (isUsable(skin.id)) {
                    UnlockResult.AlreadyOwned
                } else if (!skin.allowsPoints) {
                    UnlockResult.NotUnlockableWithPoints
                } else {
                    when (val paid = points.debitWithin(
                        amount = skin.pointsCost.toLong(),
                        type = TransactionType.SKIN_UNLOCK,
                        refId = skin.id,
                        note = skin.nameEn,
                        txId = PointsManager.unlockTxId(skin.id),
                    )) {
                        is DebitResult.Success -> {
                            storage.addOwned(skin.id, time.nowMillis(), METHOD_POINTS)
                            storage.setAdProgress(skin.id, 0) // no more ads needed once bought
                            UnlockResult.Unlocked(skin.id, paid.newBalance)
                        }
                        is DebitResult.InsufficientFunds -> UnlockResult.InsufficientPoints(paid.required, paid.available)
                        DebitResult.Duplicate -> UnlockResult.AlreadyOwned
                        is DebitResult.Failed -> UnlockResult.Failed(paid.reason)
                    }
                }
            }
        } catch (e: Exception) {
            UnlockResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Counts one verified rewarded ad towards [skinId]. Must run inside an enclosing storage transaction. */
    internal fun addAdProgressWithin(skinId: String): AdProgressResult {
        val skin = skin(skinId) ?: return AdProgressResult.NotUnlockableWithAds
        if (isUsable(skin.id)) return AdProgressResult.AlreadyOwned
        if (!skin.allowsAds) return AdProgressResult.NotUnlockableWithAds
        val done = storage.adProgress(skin.id) + 1
        return if (done >= skin.adsRequired) {
            storage.addOwned(skin.id, time.nowMillis(), METHOD_ADS)
            storage.setAdProgress(skin.id, skin.adsRequired)
            AdProgressResult.Unlocked(skin.id)
        } else {
            storage.setAdProgress(skin.id, done)
            AdProgressResult.Progress(skin.id, done, skin.adsRequired)
        }
    }

    /** Switching between usable looks is free. @return false when the look is not usable. */
    fun activate(skinId: String): Boolean {
        if (!isUsable(skinId)) return false
        storage.setActiveSkinId(skinId)
        return true
    }

    /** The active look; falls back to the default one if the stored look is no longer usable. */
    fun activeSkinId(): String {
        val stored = storage.activeSkinId() ?: return DEFAULT_SKIN_ID
        return if (isUsable(stored)) stored else DEFAULT_SKIN_ID
    }

    companion object {
        const val METHOD_POINTS = "POINTS"
        const val METHOD_ADS = "ADS"
    }
}
