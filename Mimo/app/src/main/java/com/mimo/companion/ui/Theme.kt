package com.mimo.companion.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(primary = Color(0xFF2F5BEA), secondary = Color(0xFF7A4DD8), tertiary = Color(0xFFE0457B))
private val Dark = darkColorScheme(primary = Color(0xFF9DB4FF), secondary = Color(0xFFC9B3FF), tertiary = Color(0xFFFFA8C5))

@Composable
fun MimoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (Build.VERSION.SDK_INT >= 31) {
        val c = LocalContext.current
        if (dark) dynamicDarkColorScheme(c) else dynamicLightColorScheme(c)
    } else if (dark) Dark else Light
    MaterialTheme(colorScheme = scheme, content = content)
}
