package com.mimo.companion.domain

/** Why a verified rewarded ad did not result in a reward. */
enum class RewardRejection { DISABLED, DAILY_LIMIT, TOO_SOON, DUPLICATE_EVENT, NOT_APPLICABLE }

sealed interface RewardOutcome {
    /** [points] credited to the wallet. */
    data class PointsGranted(val points: Int, val newBalance: Long) : RewardOutcome
    /** The ad counted towards (or finished) a look. */
    data class SkinProgress(val result: AdProgressResult) : RewardOutcome
    data class Rejected(val reason: RewardRejection, val retryAfterSeconds: Long = 0) : RewardOutcome
    data class Failed(val reason: String) : RewardOutcome
}

/** Read-only view for the UI: can the user earn points right now, and if not, why. */
data class EarnAvailability(val allowed: Boolean, val reason: RewardRejection? = null, val retryAfterSeconds: Long = 0, val remainingToday: Int? = null)

/**
 * Turns a VERIFIED rewarded-ad completion into a reward. It never talks to an ad SDK:
 * [com.mimo.companion.ads.RewardedAdProvider] reports a completion, and only then is one of
 * these methods called. [eventId] identifies one ad session - the same id can pay out only once.
 */
class RewardedAdsManager(
    private val storage: LedgerStorage,
    private val points: PointsManager,
    private val skins: SkinUnlockManager,
    private val config: RewardConfig,
    private val time: TimeSource = SystemTimeSource,
) {
    fun earnAvailability(): EarnAvailability {
        if (!config.rewardedAdsEnabled) return EarnAvailability(false, RewardRejection.DISABLED)
        val limitCheck = checkPointLimits()
        val remaining = if (config.dailyPointAdLimit == 0) null
        else (config.dailyPointAdLimit - storage.dailyAdCount(time.dayKey())).coerceAtLeast(0)
        return if (limitCheck == null) EarnAvailability(true, remainingToday = remaining)
        else EarnAvailability(false, limitCheck.first, limitCheck.second, remaining)
    }

    /** A completed "watch an ad, earn points" ad. */
    fun onPointsAdCompleted(eventId: String): RewardOutcome = guarded {
        if (!config.rewardedAdsEnabled) return@guarded RewardOutcome.Rejected(RewardRejection.DISABLED)
        storage.inTransaction {
            // Duplicate check first: a replayed event must never be answered with "limit reached", it simply is not paid again.
            if (!storage.markRewardEvent(eventId, time.nowMillis())) {
                return@inTransaction RewardOutcome.Rejected(RewardRejection.DUPLICATE_EVENT)
            }
            val limit = checkPointLimits()
            if (limit != null) {
                // Roll the event marker back by failing the transaction: nothing was paid, the ad can be retried properly.
                throw RejectedException(limit.first, limit.second)
            }
            when (val c = points.creditWithin(config.pointsPerAd.toLong(), TransactionType.AD_REWARD, eventId, "rewarded ad", PointsManager.adRewardTxId(eventId))) {
                is CreditResult.Success -> {
                    val day = time.dayKey()
                    storage.setDailyAdCount(day, storage.dailyAdCount(day) + 1)
                    storage.setLastAdRewardAt(time.nowMillis())
                    RewardOutcome.PointsGranted(config.pointsPerAd, c.newBalance)
                }
                CreditResult.Duplicate -> RewardOutcome.Rejected(RewardRejection.DUPLICATE_EVENT)
                is CreditResult.Failed -> throw IllegalStateException(c.reason)
            }
        }
    }

    /** A completed ad watched for a specific look ("unlock with ads"). Adds exactly one step of progress. */
    fun onSkinAdCompleted(eventId: String, skinId: String): RewardOutcome = guarded {
        if (!config.rewardedAdsEnabled) return@guarded RewardOutcome.Rejected(RewardRejection.DISABLED)
        storage.inTransaction {
            if (!storage.markRewardEvent(eventId, time.nowMillis())) {
                return@inTransaction RewardOutcome.Rejected(RewardRejection.DUPLICATE_EVENT)
            }
            when (val r = skins.addAdProgressWithin(skinId)) {
                AdProgressResult.NotUnlockableWithAds -> throw RejectedException(RewardRejection.NOT_APPLICABLE, 0)
                AdProgressResult.AlreadyOwned -> throw RejectedException(RewardRejection.NOT_APPLICABLE, 0)
                else -> RewardOutcome.SkinProgress(r)
            }
        }
    }

    private fun checkPointLimits(): Pair<RewardRejection, Long>? {
        if (config.dailyPointAdLimit > 0 && storage.dailyAdCount(time.dayKey()) >= config.dailyPointAdLimit) {
            return RewardRejection.DAILY_LIMIT to 0L
        }
        val last = storage.lastAdRewardAt()
        if (config.minSecondsBetweenPointAds > 0 && last > 0) {
            val elapsed = (time.nowMillis() - last) / 1000
            if (elapsed < config.minSecondsBetweenPointAds) {
                return RewardRejection.TOO_SOON to (config.minSecondsBetweenPointAds - elapsed)
            }
        }
        return null
    }

    private class RejectedException(val reason: RewardRejection, val retryAfter: Long) : RuntimeException()

    private inline fun guarded(block: () -> RewardOutcome): RewardOutcome = try {
        block()
    } catch (e: RejectedException) {
        RewardOutcome.Rejected(e.reason, e.retryAfter)
    } catch (e: Exception) {
        RewardOutcome.Failed(e.message ?: e.javaClass.simpleName)
    }
}
