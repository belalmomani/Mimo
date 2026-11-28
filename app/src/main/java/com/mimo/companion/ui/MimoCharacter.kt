package com.mimo.companion.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mimo.companion.Graph
import kotlinx.coroutines.delay

enum class MimoExpression { NEUTRAL, HAPPY, SAD, SURPRISED, SLEEPY, CURIOUS }

@Composable
fun MimoCharacter(
    expression: MimoExpression = MimoExpression.NEUTRAL,
    size: Float = 120f,
    onClick: () -> Unit = {}
) {
    // Blink animation
    val blinkAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2000..5000).random().toLong())
            blinkAnim.animateTo(0f, animationSpec = tween(80))
            delay(80)
            blinkAnim.animateTo(1f, animationSpec = tween(80))
        }
    }

    // Idle floating animation
    val floatAnim = rememberInfiniteTransition(label = "float")
    val floatY by floatAnim.animateFloat(
        initialValue = 0f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )

    // Speaking bob animation
    val speakAnim = rememberInfiniteTransition(label = "speak")
    val mouthOpen by speakAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        label = "mouth"
    )

    val isSpeaking by remember { derivedStateOf { Graph.isBubbleVisible.value } }

    Canvas(
        modifier = Modifier
            .size(size.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onClick() },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f + floatY
        val scale = this.size.width / 120f

        drawMimoFace(cx, cy, scale, blinkAnim.value, expression, if (isSpeaking) mouthOpen else 0f)
    }
}

private fun DrawScope.drawMimoFace(
    cx: Float, cy: Float, scale: Float,
    blinkFraction: Float,
    expression: MimoExpression,
    mouthOpen: Float
) {
    val eyeOffsetX = 20f * scale
    val eyeOffsetY = -8f * scale
    val eyeW = 18f * scale
    val eyeH = 14f * scale * blinkFraction.coerceAtLeast(0.05f)

    val eyeColor = Color(0xFF1A1A2E)
    val pupilColor = Color(0xFF4A90D9)
    val lashColor = Color(0xFF1A1A2E)

    // Left eye
    drawEye(cx - eyeOffsetX, cy + eyeOffsetY, eyeW, eyeH, eyeColor, pupilColor, lashColor, scale, expression)
    // Right eye
    drawEye(cx + eyeOffsetX, cy + eyeOffsetY, eyeW, eyeH, eyeColor, pupilColor, lashColor, scale, expression)

    // Mouth
    val mouthY = cy + 18f * scale
    val mouthW = 20f * scale
    when (expression) {
        MimoExpression.HAPPY -> drawSmile(cx, mouthY, mouthW, scale, mouthOpen)
        MimoExpression.SAD -> drawFrown(cx, mouthY, mouthW, scale)
        MimoExpression.SURPRISED -> drawCircleMouth(cx, mouthY, 8f * scale + mouthOpen * 4f * scale)
        MimoExpression.SLEEPY -> drawNeutralMouth(cx, mouthY, mouthW * 0.7f, scale)
        else -> drawSmile(cx, mouthY, mouthW * 0.6f, scale, mouthOpen * 0.5f)
    }

    // Rosy cheeks
    if (expression == MimoExpression.HAPPY || expression == MimoExpression.SURPRISED) {
        drawCircle(Color(0x33FF7777), radius = 10f * scale, center = Offset(cx - 28f * scale, cy + 10f * scale))
        drawCircle(Color(0x33FF7777), radius = 10f * scale, center = Offset(cx + 28f * scale, cy + 10f * scale))
    }
}

private fun DrawScope.drawEye(
    x: Float, y: Float, w: Float, h: Float,
    eyeColor: Color, pupilColor: Color, lashColor: Color,
    scale: Float, expression: MimoExpression
) {
    // Eye whites
    drawOval(Color.White, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h))
    // Iris
    val irisR = (w / 2.5f).coerceAtLeast(1f)
    if (h > 2f) drawCircle(pupilColor, radius = irisR, center = Offset(x, y))
    // Pupil
    if (h > 2f) drawCircle(eyeColor, radius = irisR * 0.5f, center = Offset(x, y))
    // Shine
    if (h > 2f) drawCircle(Color.White, radius = irisR * 0.2f, center = Offset(x + irisR * 0.3f, y - irisR * 0.3f))
    // Outline
    drawOval(eyeColor, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), style = Stroke(1.5f * scale))
    // Lashes (top)
    val lashStroke = Stroke(1.5f * scale, cap = StrokeCap.Round)
    for (i in 0..3) {
        val lx = x - w / 2 + (i * w / 3f)
        drawLine(lashColor, Offset(lx, y - h / 2), Offset(lx - 2f * scale, y - h / 2 - 5f * scale), strokeWidth = 1.5f * scale)
    }
}

private fun DrawScope.drawSmile(cx: Float, y: Float, w: Float, scale: Float, openFraction: Float) {
    val path = Path().apply {
        moveTo(cx - w, y)
        cubicTo(cx - w, y + 12f * scale * (1 + openFraction * 0.5f),
            cx + w, y + 12f * scale * (1 + openFraction * 0.5f), cx + w, y)
        if (openFraction > 0.1f) {
            lineTo(cx + w * 0.7f, y + 4f * scale)
            lineTo(cx - w * 0.7f, y + 4f * scale)
            close()
        }
    }
    if (openFraction > 0.1f) drawPath(path, Color(0xFFE57373))
    drawPath(path, Color(0xFF1A1A2E), style = Stroke(2f * scale, cap = StrokeCap.Round))
}

private fun DrawScope.drawFrown(cx: Float, y: Float, w: Float, scale: Float) {
    val path = Path().apply {
        moveTo(cx - w, y + 8f * scale)
        cubicTo(cx - w, y - 4f * scale, cx + w, y - 4f * scale, cx + w, y + 8f * scale)
    }
    drawPath(path, Color(0xFF1A1A2E), style = Stroke(2f * scale, cap = StrokeCap.Round))
}

private fun DrawScope.drawCircleMouth(cx: Float, y: Float, r: Float) {
    drawCircle(Color(0xFFE57373), radius = r, center = Offset(cx, y))
    drawCircle(Color(0xFF1A1A2E), radius = r, center = Offset(cx, y), style = Stroke(1.5f))
}

private fun DrawScope.drawNeutralMouth(cx: Float, y: Float, w: Float, scale: Float) {
    drawLine(Color(0xFF1A1A2E), Offset(cx - w, y), Offset(cx + w, y), strokeWidth = 2f * scale)
}
