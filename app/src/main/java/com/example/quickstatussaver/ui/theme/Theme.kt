// ui/theme/Theme.kt
package com.example.quickstatussaver.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LightGreen,
    background = LightBackground,
    surface = LightBackground,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = DarkGreen,
    background = DarkBackground,
    surface = DarkBackground,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun QuickStatusSaverTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides isDarkTheme) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
