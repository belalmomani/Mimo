package com.mimo.companion

import com.mimo.companion.domain.AdProgressResult
import com.mimo.companion.domain.CreditResult
import com.mimo.companion.domain.DEFAULT_SKIN_ID
import com.mimo.companion.domain.DebitResult
import com.mimo.companion.domain.PointsManager
import com.mimo.companion.domain.RewardConfig
import com.mimo.companion.domain.RewardOutcome
import com.mimo.companion.domain.RewardRejection
import com.mimo.companion.domain.RewardedAdsManager
import com.mimo.companion.domain.SkinCatalog
import com.mimo.companion.domain.SkinDef
import com.mimo.companion.domain.SkinStatus
import com.mimo.companion.domain.SkinUnlockManager
import com.mimo.companion.domain.TransactionType
import com.mimo.companion.domain.UnlockResult
import com.mimo.companion.domain.UnlockType
import com.mimo.companion.domain.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PointsAndStoreTest {

    private class Env(val config: RewardConfig = RewardConfig(minSecondsBetweenPointAds = 0, dailyPointAdLimit = 0, welcomeBonusPoints = 0)) {
        val storage = InMemoryLedgerStorage()
        val time = FakeTime()
        val points = PointsManager(storage, time)
        val catalog = listOf(
            skin("free", UnlockType.FREE),
            skin("ad1", UnlockType.AD_1, ads = 1),
            skin("ad2", UnlockType.AD_2, ads = 2),
            skin("ad3", UnlockType.AD_3, ads = 3),
            skin("pts", UnlockType.POINTS, points = 100),
            skin("mix", UnlockType.MIXED, ads = 2, points = 50),
        )
        val skins = SkinUnlockManager(storage, points, catalog, time)
        val ads = RewardedAdsManager(storage, points, skins, config, time)

        fun reopen(): Env {
            val e = Env(config)
            return e.also { /* fresh objects over the persisted data */ }
        }
    }

    private companion object {
        fun skin(id: String, type: UnlockType, ads: Int = 0, points: Int = 0) =
            SkinDef(id, id, id, id, Rarity.COMMON, type, ads, points)
    }

    // 1. points for a completed ad
    @Test fun completedAdGrantsConfiguredPoints() {
        val e = Env(RewardConfig(pointsPerAd = 10, minSecondsBetweenPointAds = 0, dailyPointAdLimit = 0))
        val r = e.ads.onPointsAdCompleted("evt-1")
        assertEquals(RewardOutcome.PointsGranted(10, 10), r)
        assertEquals(10L, e.points.balance())
        val tx = e.points.history().single()
        assertEquals(TransactionType.AD_REWARD, tx.type); assertEquals(10L, tx.amount); assertEquals(10L, tx.balanceAfter)
    }

    // 2. closing the ad early / ad failure -> the provider never reports completion, so nothing is booked.
    @Test fun noCompletionMeansNoRewardAndNoDeduction() {
        val e = Env()
        e.points.credit(30, TransactionType.WELCOME_BONUS)
        // (an early close or a failed load produces no call to onPointsAdCompleted / onSkinAdCompleted)
        assertEquals(30L, e.points.balance())
        assertEquals(1, e.points.history().size)
        assertEquals(0, e.storage.adProgress("ad2"))
    }

    // 3. the same ad event can never pay twice
    @Test fun sameRewardEventPaysOnlyOnce() {
        val e = Env()
        assertTrue(e.ads.onPointsAdCompleted("evt-1") is RewardOutcome.PointsGranted)
        val again = e.ads.onPointsAdCompleted("evt-1")
        assertEquals(RewardOutcome.Rejected(RewardRejection.DUPLICATE_EVENT), again)
        assertEquals(10L, e.points.balance())
        assertEquals(1, e.points.history().size)
    }

    @Test fun sameEventCannotAdvanceSkinProgressTwice() {
        val e = Env()
        e.ads.onSkinAdCompleted("evt-1", "ad2")
        val dup = e.ads.onSkinAdCompleted("evt-1", "ad2")
        assertEquals(RewardOutcome.Rejected(RewardRejection.DUPLICATE_EVENT), dup)
        assertEquals(1, e.storage.adProgress("ad2"))
    }

    // 4. one ad unlocks an AD_1 look
    @Test fun oneAdUnlocksAd1Skin() {
        val e = Env()
        assertEquals(SkinStatus.Locked, e.skins.status(e.skins.skin("ad1")!!))
        val r = e.ads.onSkinAdCompleted("e1", "ad1")
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Unlocked("ad1")), r)
        assertTrue(e.storage.isOwned("ad1"))
        assertEquals(SkinStatus.Owned, e.skins.status(e.skins.skin("ad1")!!))
    }

    // 5. two ads for AD_2: not owned after the first
    @Test fun twoAdsUnlockAd2Skin() {
        val e = Env()
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Progress("ad2", 1, 2)), e.ads.onSkinAdCompleted("e1", "ad2"))
        assertFalse(e.storage.isOwned("ad2"))
        assertEquals(SkinStatus.InProgress(1, 2), e.skins.status(e.skins.skin("ad2")!!))
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Unlocked("ad2")), e.ads.onSkinAdCompleted("e2", "ad2"))
        assertTrue(e.storage.isOwned("ad2"))
    }

    // 6. three ads, 1/3 -> 2/3 -> unlocked
    @Test fun threeAdsUnlockAd3Skin() {
        val e = Env()
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Progress("ad3", 1, 3)), e.ads.onSkinAdCompleted("e1", "ad3"))
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Progress("ad3", 2, 3)), e.ads.onSkinAdCompleted("e2", "ad3"))
        assertFalse(e.storage.isOwned("ad3"))
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Unlocked("ad3")), e.ads.onSkinAdCompleted("e3", "ad3"))
        assertTrue(e.storage.isOwned("ad3"))
        assertEquals(0L, e.points.balance()) // skin-unlock ads give progress, not points
    }

    // 7. progress survives an app restart
    @Test fun adProgressSurvivesRestart() {
        val e = Env()
        e.ads.onSkinAdCompleted("e1", "ad3"); e.ads.onSkinAdCompleted("e2", "ad3")
        val storage2 = e.storage.reopen()
        val points2 = PointsManager(storage2, e.time)
        val skins2 = SkinUnlockManager(storage2, points2, e.catalog, e.time)
        assertEquals(SkinStatus.InProgress(2, 3), skins2.status(e.catalog.first { it.id == "ad3" }))
        val ads2 = RewardedAdsManager(storage2, points2, skins2, e.config, e.time)
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Unlocked("ad3")), ads2.onSkinAdCompleted("e3", "ad3"))
    }

    // 8. unlock with points
    @Test fun unlockWithPointsChargesOnceAndGrantsOwnership() {
        val e = Env()
        e.points.credit(150, TransactionType.SPECIAL_EVENT)
        val r = e.skins.unlockWithPoints("pts")
        assertEquals(UnlockResult.Unlocked("pts", 50), r)
        assertEquals(50L, e.points.balance())
        assertTrue(e.storage.isOwned("pts"))
        val unlockTx = e.points.history().first()
        assertEquals(TransactionType.SKIN_UNLOCK, unlockTx.type); assertEquals(-100L, unlockTx.amount)
    }

    // 9. never negative
    @Test fun insufficientBalanceChargesNothing() {
        val e = Env()
        e.points.credit(40, TransactionType.SPECIAL_EVENT)
        assertEquals(UnlockResult.InsufficientPoints(100, 40), e.skins.unlockWithPoints("pts"))
        assertEquals(40L, e.points.balance()); assertFalse(e.storage.isOwned("pts"))
        assertEquals(1, e.points.history().size)
        assertEquals(DebitResult.InsufficientFunds(5, 0), PointsManager(InMemoryLedgerStorage(), e.time).debit(5, TransactionType.SKIN_UNLOCK))
    }

    // 10. double tap on "unlock" charges once
    @Test fun repeatedUnlockTapChargesOnce() {
        val e = Env()
        e.points.credit(300, TransactionType.SPECIAL_EVENT)
        assertTrue(e.skins.unlockWithPoints("pts") is UnlockResult.Unlocked)
        assertEquals(UnlockResult.AlreadyOwned, e.skins.unlockWithPoints("pts"))
        assertEquals(UnlockResult.AlreadyOwned, e.skins.unlockWithPoints("pts"))
        assertEquals(200L, e.points.balance())
        assertEquals(2, e.points.history().size) // credit + exactly one unlock
    }

    @Test fun sameDebitTransactionIdIsBookedOnce() {
        val e = Env()
        e.points.credit(100, TransactionType.SPECIAL_EVENT)
        assertTrue(e.points.debit(30, TransactionType.SKIN_UNLOCK, txId = "t1") is DebitResult.Success)
        assertEquals(DebitResult.Duplicate, e.points.debit(30, TransactionType.SKIN_UNLOCK, txId = "t1"))
        assertEquals(70L, e.points.balance())
    }

    // 11. already owned / free: no charge, no ads
    @Test fun ownedOrFreeSkinCostsNothing() {
        val e = Env()
        e.points.credit(500, TransactionType.SPECIAL_EVENT)
        e.skins.unlockWithPoints("pts")
        val before = e.points.balance()
        assertEquals(UnlockResult.AlreadyOwned, e.skins.unlockWithPoints("pts"))
        assertEquals(UnlockResult.AlreadyOwned, e.skins.unlockWithPoints("free"))
        assertEquals(before, e.points.balance())
        assertEquals(RewardOutcome.Rejected(RewardRejection.NOT_APPLICABLE), e.ads.onSkinAdCompleted("e1", "pts"))
    }

    @Test fun pointsOnlyAndAdOnlyRulesAreEnforced() {
        val e = Env()
        e.points.credit(500, TransactionType.SPECIAL_EVENT)
        assertEquals(UnlockResult.NotUnlockableWithPoints, e.skins.unlockWithPoints("ad2"))
        assertEquals(RewardOutcome.Rejected(RewardRejection.NOT_APPLICABLE), e.ads.onSkinAdCompleted("e1", "pts"))
        assertEquals(UnlockResult.UnknownSkin, e.skins.unlockWithPoints("nope"))
        assertEquals(500L, e.points.balance())
    }

    @Test fun mixedSkinCanBeBoughtAfterPartialAdProgress() {
        val e = Env()
        e.ads.onSkinAdCompleted("e1", "mix")
        e.points.credit(80, TransactionType.SPECIAL_EVENT)
        assertTrue(e.skins.unlockWithPoints("mix") is UnlockResult.Unlocked)
        assertEquals(SkinStatus.Owned, e.skins.status(e.skins.skin("mix")!!))
        assertEquals(RewardOutcome.Rejected(RewardRejection.NOT_APPLICABLE), e.ads.onSkinAdCompleted("e2", "mix"))
    }

    // 12/13. failures: nothing is booked (no ad -> nothing; ad SDK offline -> nothing). Rejections:
    @Test fun disabledRewardedAdsGrantNothing() {
        val e = Env(RewardConfig(rewardedAdsEnabled = false))
        assertEquals(RewardOutcome.Rejected(RewardRejection.DISABLED), e.ads.onPointsAdCompleted("e1"))
        assertEquals(RewardOutcome.Rejected(RewardRejection.DISABLED), e.ads.onSkinAdCompleted("e2", "ad1"))
        assertEquals(0L, e.points.balance()); assertFalse(e.storage.isOwned("ad1"))
        assertFalse(e.ads.earnAvailability().allowed)
    }

    // 14. balance persists after restart
    @Test fun balanceAndOwnershipSurviveRestart() {
        val e = Env()
        e.points.credit(200, TransactionType.SPECIAL_EVENT)
        e.skins.unlockWithPoints("pts"); e.skins.activate("pts")
        val storage2 = e.storage.reopen()
        val points2 = PointsManager(storage2, e.time)
        val skins2 = SkinUnlockManager(storage2, points2, e.catalog, e.time)
        assertEquals(100L, points2.balance())
        assertTrue(skins2.isUsable("pts"))
        assertEquals("pts", skins2.activeSkinId())
        assertEquals(2, points2.history().size)
    }

    // 15. ownership is not tied to the store UI/catalog order: a reshuffled or extended catalog keeps it
    @Test fun ownershipSurvivesCatalogChange() {
        val e = Env()
        e.points.credit(200, TransactionType.SPECIAL_EVENT)
        e.skins.unlockWithPoints("pts")
        val newCatalog = e.catalog.reversed() + skin("brand-new", UnlockType.AD_1, ads = 1)
        val skins2 = SkinUnlockManager(e.storage.reopen(), PointsManager(e.storage.reopen(), e.time), newCatalog, e.time)
        assertTrue(skins2.isUsable("pts"))
        assertFalse(skins2.isUsable("brand-new"))
    }

    // 16. a failing write rolls everything back
    @Test fun failedLedgerWriteChargesNothing() {
        val e = Env()
        e.points.credit(200, TransactionType.SPECIAL_EVENT)
        e.storage.failNextInsertTransaction = true
        val r = e.skins.unlockWithPoints("pts")
        assertTrue(r is UnlockResult.Failed)
        assertEquals(200L, e.points.balance()); assertFalse(e.storage.isOwned("pts"))
        assertEquals(1, e.points.history().size)
    }

    @Test fun failureAfterChargeRollsTheChargeBack() {
        val e = Env()
        e.points.credit(200, TransactionType.SPECIAL_EVENT)
        e.storage.failNextAddOwned = true // fails AFTER the debit was written inside the transaction
        assertTrue(e.skins.unlockWithPoints("pts") is UnlockResult.Failed)
        assertEquals(200L, e.points.balance()); assertFalse(e.storage.isOwned("pts"))
        assertEquals(1, e.points.history().size)
        // and the user can still buy it afterwards
        assertTrue(e.skins.unlockWithPoints("pts") is UnlockResult.Unlocked)
        assertEquals(100L, e.points.balance())
    }

    @Test fun failedRewardCreditDoesNotBurnTheEvent() {
        val e = Env()
        e.storage.failNextInsertTransaction = true
        assertTrue(e.ads.onPointsAdCompleted("evt-1") is RewardOutcome.Failed)
        assertEquals(0L, e.points.balance())
        // the event marker was rolled back with the transaction, so a retry of the same event still pays
        assertTrue(e.ads.onPointsAdCompleted("evt-1") is RewardOutcome.PointsGranted)
    }

    // limits
    @Test fun dailyLimitBlocksFurtherPointAdsAndResetsNextDay() {
        val e = Env(RewardConfig(pointsPerAd = 10, dailyPointAdLimit = 2, minSecondsBetweenPointAds = 0))
        assertTrue(e.ads.onPointsAdCompleted("a") is RewardOutcome.PointsGranted)
        assertTrue(e.ads.onPointsAdCompleted("b") is RewardOutcome.PointsGranted)
        assertEquals(RewardOutcome.Rejected(RewardRejection.DAILY_LIMIT), e.ads.onPointsAdCompleted("c"))
        assertEquals(20L, e.points.balance())
        assertEquals(RewardRejection.DAILY_LIMIT, e.ads.earnAvailability().reason)
        e.time.advanceDays(1)
        assertTrue(e.ads.earnAvailability().allowed)
        assertTrue(e.ads.onPointsAdCompleted("c") is RewardOutcome.PointsGranted) // same id as the rejected one: it was not burned
    }

    @Test fun minimumIntervalBetweenPointAds() {
        val e = Env(RewardConfig(pointsPerAd = 10, dailyPointAdLimit = 0, minSecondsBetweenPointAds = 30))
        assertTrue(e.ads.onPointsAdCompleted("a") is RewardOutcome.PointsGranted)
        e.time.advanceSeconds(10)
        assertEquals(RewardOutcome.Rejected(RewardRejection.TOO_SOON, 20), e.ads.onPointsAdCompleted("b"))
        e.time.advanceSeconds(20)
        assertTrue(e.ads.onPointsAdCompleted("b") is RewardOutcome.PointsGranted)
    }

    @Test fun skinAdsAreNotBlockedByPointAdLimits() {
        val e = Env(RewardConfig(dailyPointAdLimit = 1, minSecondsBetweenPointAds = 60))
        e.ads.onPointsAdCompleted("p1")
        assertEquals(RewardOutcome.SkinProgress(AdProgressResult.Unlocked("ad1")), e.ads.onSkinAdCompleted("s1", "ad1"))
    }

    // welcome bonus / adjustments
    @Test fun welcomeBonusIsGrantedOnlyOnce() {
        val e = Env()
        assertEquals(CreditResult.Success(20), e.points.grantWelcomeBonus(20))
        assertEquals(CreditResult.Duplicate, e.points.grantWelcomeBonus(20))
        assertEquals(CreditResult.Duplicate, PointsManager(e.storage.reopen(), e.time).grantWelcomeBonus(20))
        assertEquals(20L, e.points.balance())
    }

    @Test fun adjustmentsRespectTheNonNegativeRule() {
        val e = Env()
        e.points.credit(50, TransactionType.SPECIAL_EVENT)
        assertTrue(e.points.adjust(-80, "correction", "adj-1") is DebitResult.InsufficientFunds)
        assertEquals(50L, e.points.balance())
        assertTrue(e.points.adjust(-20, "correction", "adj-2") is DebitResult.Success)
        assertEquals(30L, e.points.balance())
    }

    @Test fun nonPositiveAmountsAreRejected() {
        val e = Env()
        assertTrue(e.points.credit(0, TransactionType.SPECIAL_EVENT) is CreditResult.Failed)
        assertTrue(e.points.credit(-5, TransactionType.SPECIAL_EVENT) is CreditResult.Failed)
        assertTrue(e.points.debit(0, TransactionType.SKIN_UNLOCK) is DebitResult.Failed)
        assertEquals(0L, e.points.balance())
    }

    // active look
    @Test fun activatingAndSwitchingIsFreeAndRequiresOwnership() {
        val e = Env()
        e.points.credit(200, TransactionType.SPECIAL_EVENT)
        assertEquals(DEFAULT_SKIN_ID, e.skins.activeSkinId())
        assertFalse(e.skins.activate("pts"))
        e.skins.unlockWithPoints("pts")
        val bal = e.points.balance()
        assertTrue(e.skins.activate("pts")); assertTrue(e.skins.activate("free")); assertTrue(e.skins.activate(DEFAULT_SKIN_ID))
        assertEquals(bal, e.points.balance())
    }

    // ledger consistency: balanceAfter chain adds up
    @Test fun ledgerAlwaysMatchesBalance() {
        val e = Env()
        e.ads.onPointsAdCompleted("a"); e.ads.onPointsAdCompleted("b"); e.points.credit(100, TransactionType.SPECIAL_EVENT)
        e.skins.unlockWithPoints("pts")
        e.points.debit(5, TransactionType.SKIN_UNLOCK, "x")
        val sum = e.points.history(1000).sumOf { it.amount }
        assertEquals(e.points.balance(), sum)
        assertEquals(e.points.balance(), e.points.history().first().balanceAfter)
        assertTrue(e.points.history().all { it.balanceAfter >= 0 })
    }

    // catalog sanity (the real one)
    @Test fun realCatalogHasTwentyValidLooksWithRequestedDistribution() {
        val c = SkinCatalog.ALL
        assertEquals(20, c.size)
        assertEquals(20, c.map { it.id }.toSet().size)
        assertEquals(UnlockType.FREE, c[0].unlockType)
        assertEquals(listOf(UnlockType.AD_1, UnlockType.AD_1), c.subList(1, 3).map { it.unlockType })
        assertEquals(UnlockType.AD_5, c[8].unlockType); assertEquals(UnlockType.POINTS, c[9].unlockType)
        assertEquals(UnlockType.POINTS, c[14].unlockType)
        assertTrue(c.map { it.adsRequired }.toSet().containsAll(setOf(0, 1, 2, 3, 4, 5)))
        assertNotNull(c.firstOrNull { it.featured })
        assertTrue(c.map { it.rarity }.toSet().size >= 4)
    }
}
