package com.mimo.companion.domain

import java.time.Instant
import java.time.ZoneId

/** Id of the built-in default look (the base icon). It is always usable and is not part of the store catalog. */
const val DEFAULT_SKIN_ID = "default"

/** Supplies "now". Injected so that daily limits and intervals can be tested deterministically. */
interface TimeSource {
    fun nowMillis(): Long
    fun zone(): ZoneId = ZoneId.systemDefault()
}

object SystemTimeSource : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** Calendar-day key in the device time zone, e.g. "2026-10-26". Used for the daily rewarded-ad limit. */
fun TimeSource.dayKey(): String =
    Instant.ofEpochMilli(nowMillis()).atZone(zone()).toLocalDate().toString()

enum class TransactionType {
    AD_REWARD,
    SKIN_UNLOCK,
    POINTS_PURCHASE,
    WELCOME_BONUS,
    SPECIAL_EVENT,
    REFUND_OR_ADJUSTMENT,
}

/**
 * One immutable ledger row. [id] is unique across the whole ledger and doubles as the
 * idempotency key: inserting the same id twice is rejected by the storage.
 */
data class PointsTransaction(
    val id: String,
    val type: TransactionType,
    /** Positive = credit, negative = debit. */
    val amount: Long,
    val balanceAfter: Long,
    /** Skin id, ad-session id or purchase order id this row relates to. */
    val refId: String?,
    val note: String?,
    val createdAtMillis: Long,
)

data class OwnedSkin(
    val skinId: String,
    val unlockedAtMillis: Long,
    /** "ADS" or "POINTS". */
    val method: String,
)

enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

enum class UnlockType { FREE, AD_1, AD_2, AD_3, AD_4, AD_5, POINTS, MIXED }

/**
 * A store item. Everything a designer may want to tune (ads needed, points cost, rarity)
 * lives here and is filled in [SkinCatalog] - the single configuration file for the store.
 */
data class SkinDef(
    val id: String,
    /** Drawable resource name of the art, e.g. "face_01" (res/drawable-nodpi/face_01.png). */
    val art: String,
    val nameAr: String,
    val nameEn: String,
    val rarity: Rarity,
    val unlockType: UnlockType,
    /** Rewarded ads needed (AD_n and MIXED); 0 otherwise. */
    val adsRequired: Int,
    /** Points price (POINTS and MIXED); 0 otherwise. */
    val pointsCost: Int,
    /** Where the eyes are, as a fraction of the image height; null = this look does not blink. */
    val blinkPivotY: Float? = null,
    /** The image is split here (fraction of height): above = eyes (blink), below = mouth. */
    val blinkSplitY: Float? = null,
    val featured: Boolean = false,
) {
    val isFree: Boolean get() = unlockType == UnlockType.FREE
    val allowsAds: Boolean
        get() = when (unlockType) {
            UnlockType.AD_1, UnlockType.AD_2, UnlockType.AD_3, UnlockType.AD_4, UnlockType.AD_5, UnlockType.MIXED -> true
            else -> false
        }
    val allowsPoints: Boolean get() = unlockType == UnlockType.POINTS || unlockType == UnlockType.MIXED
    val canBlink: Boolean get() = blinkPivotY != null && blinkSplitY != null

    init {
        require(id.isNotBlank() && id != DEFAULT_SKIN_ID) { "invalid skin id '$id'" }
        when (unlockType) {
            UnlockType.FREE -> require(adsRequired == 0 && pointsCost == 0) { "$id: FREE must not have a price" }
            UnlockType.AD_1 -> require(adsRequired == 1 && pointsCost == 0) { "$id: AD_1 needs exactly 1 ad" }
            UnlockType.AD_2 -> require(adsRequired == 2 && pointsCost == 0) { "$id: AD_2 needs exactly 2 ads" }
            UnlockType.AD_3 -> require(adsRequired == 3 && pointsCost == 0) { "$id: AD_3 needs exactly 3 ads" }
            UnlockType.AD_4 -> require(adsRequired == 4 && pointsCost == 0) { "$id: AD_4 needs exactly 4 ads" }
            UnlockType.AD_5 -> require(adsRequired == 5 && pointsCost == 0) { "$id: AD_5 needs exactly 5 ads" }
            UnlockType.POINTS -> require(adsRequired == 0 && pointsCost > 0) { "$id: POINTS needs a cost" }
            UnlockType.MIXED -> require(adsRequired > 0 && pointsCost > 0) { "$id: MIXED needs ads and a cost" }
        }
        require((blinkPivotY == null) == (blinkSplitY == null)) { "$id: blink pivot and split go together" }
    }
}

sealed interface SkinStatus {
    object Free : SkinStatus
    object Owned : SkinStatus
    data class InProgress(val done: Int, val total: Int) : SkinStatus
    object Locked : SkinStatus
}

// ---- results ---------------------------------------------------------------------------------

sealed interface CreditResult {
    data class Success(val newBalance: Long) : CreditResult
    /** Same transaction id was already booked: nothing changed. */
    object Duplicate : CreditResult
    data class Failed(val reason: String) : CreditResult
}

sealed interface DebitResult {
    data class Success(val newBalance: Long) : DebitResult
    data class InsufficientFunds(val required: Long, val available: Long) : DebitResult
    object Duplicate : DebitResult
    data class Failed(val reason: String) : DebitResult
}

sealed interface UnlockResult {
    data class Unlocked(val skinId: String, val newBalance: Long) : UnlockResult
    object AlreadyOwned : UnlockResult
    data class InsufficientPoints(val required: Long, val available: Long) : UnlockResult
    object NotUnlockableWithPoints : UnlockResult
    object UnknownSkin : UnlockResult
    data class Failed(val reason: String) : UnlockResult
}

sealed interface AdProgressResult {
    data class Progress(val skinId: String, val done: Int, val total: Int) : AdProgressResult
    data class Unlocked(val skinId: String) : AdProgressResult
    object AlreadyOwned : AdProgressResult
    object NotUnlockableWithAds : AdProgressResult
}
