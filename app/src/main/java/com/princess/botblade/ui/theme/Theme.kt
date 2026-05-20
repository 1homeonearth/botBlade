package com.princess.botblade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeedPurple = Color(0xFF9B59FF)

private val LightColors = lightColorScheme(
    primary = SeedPurple,
    secondary = Color(0xFF6E5B8F),
    tertiary = Color(0xFF7A5C9B),
)

private val DarkColors = darkColorScheme(
    primary = SeedPurple,
    secondary = Color(0xFFC6B4E3),
    tertiary = Color(0xFFDFC2FF),
)

@Composable
fun BotBladeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
