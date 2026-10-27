package com.mimo.companion.face

import androidx.annotation.DrawableRes
import com.mimo.companion.R
import com.mimo.companion.domain.DEFAULT_SKIN_ID
import com.mimo.companion.domain.SkinCatalog

/**
 * Static resource lookup (instead of Resources.getIdentifier) so that R8 resource shrinking
 * can see which drawables are used. When you add face_21.png also add it to [FACES] below.
 */
object SkinArt {
    private val FACES: Map<String, Int> = mapOf(
        "face_01" to R.drawable.face_01, "face_02" to R.drawable.face_02, "face_03" to R.drawable.face_03,
        "face_04" to R.drawable.face_04, "face_05" to R.drawable.face_05, "face_06" to R.drawable.face_06,
        "face_07" to R.drawable.face_07, "face_08" to R.drawable.face_08, "face_09" to R.drawable.face_09,
        "face_10" to R.drawable.face_10, "face_11" to R.drawable.face_11, "face_12" to R.drawable.face_12,
        "face_13" to R.drawable.face_13, "face_14" to R.drawable.face_14, "face_15" to R.drawable.face_15,
        "face_16" to R.drawable.face_16, "face_17" to R.drawable.face_17, "face_18" to R.drawable.face_18,
        "face_19" to R.drawable.face_19, "face_20" to R.drawable.face_20,
    )

    /** Blink geometry of the default look (fractions of the 512px canvas; measured from the base artwork). */
    const val DEFAULT_PIVOT_Y = 0.40f
    const val DEFAULT_SPLIT_Y = 0.60f

    @DrawableRes fun resFor(skinId: String): Int {
        if (skinId == DEFAULT_SKIN_ID) return R.drawable.mimo_base
        val art = SkinCatalog.ALL.firstOrNull { it.id == skinId }?.art ?: return R.drawable.mimo_base
        return FACES[art] ?: R.drawable.mimo_base
    }

    /** (pivotY, splitY) or null when this look should not blink. */
    fun blinkFor(skinId: String): Pair<Float, Float>? {
        if (skinId == DEFAULT_SKIN_ID) return DEFAULT_PIVOT_Y to DEFAULT_SPLIT_Y
        val s = SkinCatalog.ALL.firstOrNull { it.id == skinId } ?: return null
        return if (s.canBlink) s.blinkPivotY!! to s.blinkSplitY!! else null
    }
}
