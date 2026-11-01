package com.mimo.companion.domain

/**
 * Central, tunable economy settings. Change values here (or build a different instance) -
 * no screen hard-codes any of these numbers.
 */
data class RewardConfig(
    /** Mimo Points granted for one completed "watch an ad, earn points" ad. */
    val pointsPerAd: Int = 10,
    /** Max completed point-earning ads per calendar day. 0 = unlimited. */
    val dailyPointAdLimit: Int = 20,
    /** Minimum time between two point-earning rewards. 0 = no interval. */
    val minSecondsBetweenPointAds: Int = 30,
    /** Master switch for every rewarded ad (points AND ad-unlock progress). */
    val rewardedAdsEnabled: Boolean = true,
    /** One-time welcome bonus. 0 disables it. */
    val welcomeBonusPoints: Int = 20,
) {
    init {
        require(pointsPerAd > 0) { "pointsPerAd must be positive" }
        require(dailyPointAdLimit >= 0 && minSecondsBetweenPointAds >= 0 && welcomeBonusPoints >= 0)
    }
}

/** Store packs for real-money purchases. Prices come from Google Play (never hard-coded here). */
data class PointsPack(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val points: Int,
    /** Play Console in-app product id. Intentionally unset until the products exist in the console. */
    val playProductId: String? = null,
)

object PointsPacks {
    val ALL = listOf(
        PointsPack("starter", "Starter", "المبتدئ", 100),
        PointsPack("plus", "Plus", "بلس", 300),
        PointsPack("premium", "Premium", "بريميوم", 700),
        PointsPack("ultimate", "Ultimate", "ألتميت", 1500),
    )
}
