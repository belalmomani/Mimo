package com.mimo.companion.face

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Draws one look and (when the look supports it) blinks at irregular intervals.
 *
 * Blink = the upper band of the image (the eyes) is squashed vertically around the eye line while
 * the lower band (the mouth) is drawn untouched. It is a cheap approximation of a real blink
 * (one animated float, ~100 ms every 2.5-6 s, nothing running while idle), not a hand-animated one.
 * It is skipped when the user turned system animations off.
 */
@Composable
fun MimoFace(skinId: String, modifier: Modifier = Modifier, blink: Boolean = true) {
    val context = LocalContext.current
    val image: ImageBitmap = ImageBitmap.imageResource(SkinArt.resFor(skinId))
    val blinkSpec = remember(skinId) { SkinArt.blinkFor(skinId) }
    val eyeOpen = remember(skinId) { Animatable(1f) }

    LaunchedEffect(skinId, blink) {
        if (!blink || blinkSpec == null) return@LaunchedEffect
        val animationsOn = try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        } catch (_: Exception) { true }
        if (!animationsOn) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2_500, 6_000))
            repeat(if (Random.nextInt(5) == 0) 2 else 1) {          // occasional double blink
                eyeOpen.animateTo(0.06f, tween(70, easing = FastOutSlowInEasing))
                eyeOpen.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            }
        }
    }

    Canvas(modifier.aspectRatio(1f)) {
        val dst = IntSize(size.width.toInt(), size.height.toInt())
        val open = eyeOpen.value
        if (blinkSpec == null || open >= 0.999f) {
            drawImage(image, dstSize = dst)
        } else {
            val (pivot, split) = blinkSpec
            val splitPx = split * size.height
            clipRect(0f, splitPx, size.width, size.height) { drawImage(image, dstSize = dst) }          // mouth
            clipRect(0f, 0f, size.width, splitPx) {                                                       // eyes
                scale(1f, open, pivot = Offset(size.width / 2f, pivot * size.height)) { drawImage(image, dstSize = dst) }
            }
        }
    }
}
