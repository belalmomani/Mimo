package com.mimo.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.companion.Graph

@Composable
fun FloatingOverlay(onTap: () -> Unit) {
    val bubbleText by Graph.bubbleText
    val bubbleVisible by Graph.isBubbleVisible

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Speech bubble
        AnimatedVisibility(visible = bubbleVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(bubbleText, fontSize = 13.sp, color = Color.Black, lineHeight = 18.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Mimo character
        Box(modifier = Modifier.clickable { onTap() }) {
            MimoCharacter(
                expression = expressionFrom(Graph.characterExpression.value),
                size = 90f,
                onClick = onTap
            )
        }
    }
}

private fun expressionFrom(key: String) = when (key) {
    "happy" -> MimoExpression.HAPPY
    "sad" -> MimoExpression.SAD
    "surprised" -> MimoExpression.SURPRISED
    "sleepy" -> MimoExpression.SLEEPY
    "curious" -> MimoExpression.CURIOUS
    else -> MimoExpression.NEUTRAL
}
