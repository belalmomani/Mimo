package com.mimo.companion.store

object ShapeCatalog {
    val all: List<MimoShape> = listOf(
        MimoShape("s01", "الكلاسيكي", "😊", UnlockType.FREE, rarity = Rarity.COMMON),
        MimoShape("s02", "الفرحان", "😄", UnlockType.AD_1, rarity = Rarity.COMMON),
        MimoShape("s03", "المبتسم", "🙂", UnlockType.AD_1, rarity = Rarity.COMMON),
        MimoShape("s04", "الدهشة", "😲", UnlockType.AD_2, rarity = Rarity.COMMON),
        MimoShape("s05", "الفضولي", "🤔", UnlockType.AD_2, rarity = Rarity.COMMON),
        MimoShape("s06", "المتعب", "😴", UnlockType.AD_3, rarity = Rarity.RARE),
        MimoShape("s07", "الحزين", "😢", UnlockType.AD_3, rarity = Rarity.RARE),
        MimoShape("s08", "الغاضب", "😤", UnlockType.AD_4, rarity = Rarity.RARE),
        MimoShape("s09", "المتحمس", "🤩", UnlockType.AD_5, rarity = Rarity.EPIC),
        MimoShape("s10", "الملكي", "👑", UnlockType.POINTS, pointsCost = 50, rarity = Rarity.RARE),
        MimoShape("s11", "النجمة", "⭐", UnlockType.MIXED, pointsCost = 30, adsRequired = 1, rarity = Rarity.RARE),
        MimoShape("s12", "الحب", "🥰", UnlockType.MIXED, pointsCost = 50, adsRequired = 2, rarity = Rarity.RARE),
        MimoShape("s13", "البرق", "⚡", UnlockType.MIXED, pointsCost = 80, adsRequired = 3, rarity = Rarity.EPIC),
        MimoShape("s14", "المتسلق", "🚀", UnlockType.MIXED, pointsCost = 120, adsRequired = 5, rarity = Rarity.EPIC),
        MimoShape("s15", "الذهبي", "🏆", UnlockType.POINTS, pointsCost = 200, rarity = Rarity.LEGENDARY),
        MimoShape("s16", "الليلي", "🌙", UnlockType.AD_1, rarity = Rarity.COMMON),
        MimoShape("s17", "الصيفي", "☀️", UnlockType.AD_2, rarity = Rarity.COMMON),
        MimoShape("s18", "الشتوي", "❄️", UnlockType.AD_3, rarity = Rarity.RARE),
        MimoShape("s19", "الربيعي", "🌸", UnlockType.AD_4, rarity = Rarity.RARE),
        MimoShape("s20", "الأسطوري", "🌟", UnlockType.MIXED, pointsCost = 300, adsRequired = 5, rarity = Rarity.LEGENDARY),
    )

    fun byId(id: String) = all.find { it.id == id }
}
