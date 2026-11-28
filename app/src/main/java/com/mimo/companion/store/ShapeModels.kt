package com.mimo.companion.store

enum class UnlockType {
    FREE,   // مجاني
    AD_1, AD_2, AD_3, AD_4, AD_5,  // عدد الإعلانات
    POINTS, // نقاط فقط
    MIXED   // نقاط أو إعلانات
}

data class MimoShape(
    val id: String,
    val nameAr: String,
    val emoji: String,           // للعرض بدلاً من صورة خارجية
    val unlockType: UnlockType,
    val pointsCost: Int = 0,     // 0 إذا لم يكن POINTS/MIXED
    val adsRequired: Int = 0,    // من UnlockType تلقائياً
    val rarity: Rarity = Rarity.COMMON
) {
    val adCount: Int get() = when (unlockType) {
        UnlockType.AD_1 -> 1; UnlockType.AD_2 -> 2; UnlockType.AD_3 -> 3
        UnlockType.AD_4 -> 4; UnlockType.AD_5 -> 5
        UnlockType.MIXED -> adsRequired
        else -> 0
    }
    val isFree get() = unlockType == UnlockType.FREE
    val canUsePoints get() = unlockType == UnlockType.POINTS || unlockType == UnlockType.MIXED
    val needsAds get() = adCount > 0
}

enum class Rarity(val labelAr: String, val color: Long) {
    COMMON("عادي", 0xFF9E9E9E),
    RARE("نادر", 0xFF2196F3),
    EPIC("أسطوري", 0xFF9C27B0),
    LEGENDARY("خرافي", 0xFFFF9800)
}

data class ShapeOwnership(
    val shapeId: String,
    val adsWatched: Int = 0,    // تقدم الإعلانات
    val owned: Boolean = false,
    val unlockedAt: Long = 0L
)
