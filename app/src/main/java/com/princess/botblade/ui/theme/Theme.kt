package com.princess.botblade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SeedPurple = Color(0xFF8F5BFF)
private val GoldPrimary = Color(0xFFFFD166)

private val LightColors = lightColorScheme(
    primary = SeedPurple,
    secondary = GoldPrimary,
    tertiary = Color(0xFF5F5A78),
    surface = Color(0xFFF6F2FF),
    surfaceVariant = Color(0xFFE8E0F8),
)

private val DarkColors = darkColorScheme(
    primary = SeedPurple,
    secondary = GoldPrimary,
    tertiary = Color(0xFFCCC2FF),
    surface = Color(0xFF0D0A17),
    surfaceVariant = Color(0xFF2A2438),
)

@Composable
fun BotBladeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
