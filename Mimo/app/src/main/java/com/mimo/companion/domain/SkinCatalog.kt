package com.mimo.companion.domain

import com.mimo.companion.domain.Rarity.COMMON
import com.mimo.companion.domain.Rarity.EPIC
import com.mimo.companion.domain.Rarity.LEGENDARY
import com.mimo.companion.domain.Rarity.RARE
import com.mimo.companion.domain.Rarity.UNCOMMON
import com.mimo.companion.domain.UnlockType.AD_1
import com.mimo.companion.domain.UnlockType.AD_2
import com.mimo.companion.domain.UnlockType.AD_3
import com.mimo.companion.domain.UnlockType.AD_4
import com.mimo.companion.domain.UnlockType.AD_5
import com.mimo.companion.domain.UnlockType.FREE
import com.mimo.companion.domain.UnlockType.MIXED
import com.mimo.companion.domain.UnlockType.POINTS

/**
 * THE store configuration file. One row per look.
 *
 *  - change how many ads / how many points a look costs: edit its row (ads = N for AD_n/MIXED, points = P);
 *  - add a look: drop `face_21.png` into res/drawable-nodpi and add a row with id "face_21", art = "face_21";
 *  - the order of rows is the order shown in the store.
 *
 * `blink` = (eyeCenterY, splitY) fractions of the image height, generated into tools/face_meta.json
 * by tools/slice_sheet.py. Looks without eyes that can blink (winks, closed eyes, sunglasses) use null.
 *
 * Looks 1..20 follow the distribution requested for the store (look 1 free, 2-3 one ad, ...).
 */
object SkinCatalog {

    private fun s(
        n: Int, ar: String, en: String, rarity: Rarity, type: UnlockType,
        ads: Int = 0, points: Int = 0, blink: Pair<Float, Float>? = null, featured: Boolean = false,
    ) = SkinDef(
        id = "face_%02d".format(n), art = "face_%02d".format(n), nameAr = ar, nameEn = en,
        rarity = rarity, unlockType = type, adsRequired = ads, pointsCost = points,
        blinkPivotY = blink?.first, blinkSplitY = blink?.second, featured = featured,
    )

    val ALL: List<SkinDef> = listOf(
        s(1, "ابتسامة هادئة", "Calm Smile", COMMON, FREE, blink = 0.513f to 0.676f),
        s(2, "غمزة", "Wink", COMMON, AD_1, ads = 1, blink = 0.488f to 0.631f),
        s(3, "دهشة", "Surprised", COMMON, AD_1, ads = 1, blink = 0.476f to 0.628f),
        s(4, "نظرة جانبية", "Side Glance", UNCOMMON, AD_2, ads = 2, blink = 0.518f to 0.669f),
        s(5, "قلق", "Worried", UNCOMMON, AD_2, ads = 2, blink = 0.547f to 0.700f),
        s(6, "استرخاء", "Relaxed", UNCOMMON, AD_3, ads = 3),
        s(7, "مرح", "Playful", RARE, AD_3, ads = 3, blink = 0.494f to 0.652f),
        s(8, "ضحكة", "Big Laugh", RARE, AD_4, ads = 4),
        s(9, "فضول", "Curious", EPIC, AD_5, ads = 5, blink = 0.536f to 0.723f),
        s(10, "حازم", "Determined", RARE, POINTS, points = 60, blink = 0.514f to 0.657f),
        s(11, "فضول هادئ", "Quiet Curiosity", UNCOMMON, MIXED, ads = 1, points = 20, blink = 0.496f to 0.642f),
        s(12, "ابتسامة جانبية", "Smirk", UNCOMMON, MIXED, ads = 2, points = 40, blink = 0.518f to 0.668f),
        s(13, "حيرة", "Puzzled", RARE, MIXED, ads = 3, points = 60, blink = 0.512f to 0.688f),
        s(14, "فرح", "Joy", EPIC, MIXED, ads = 5, points = 100),
        s(15, "تردد", "Hesitant", RARE, POINTS, points = 80, blink = 0.556f to 0.718f),
        s(16, "نظارة شمسية", "Cool Shades", UNCOMMON, AD_1, ads = 1),
        s(17, "قبلة", "Blow a Kiss", RARE, AD_2, ads = 2, blink = 0.498f to 0.639f),
        s(18, "إرهاق", "Tired", RARE, AD_3, ads = 3, blink = 0.548f to 0.727f),
        s(19, "تثاؤب", "Yawn", EPIC, AD_4, ads = 4),
        s(20, "عيون النجوم", "Star Eyes", LEGENDARY, MIXED, ads = 5, points = 150, blink = 0.516f to 0.686f, featured = true),
    )

    init {
        require(ALL.map { it.id }.toSet().size == ALL.size) { "duplicate skin ids in SkinCatalog" }
    }
}
