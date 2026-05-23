package com.princess.botblade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BotBladeBlack = Color(0xFF05060A)
private val BotBladeInk = Color(0xFF090B12)
private val BotBladePanel = Color(0xFF101522)
private val BotBladeRaised = Color(0xFF151D2E)
private val BotBladeBabyBlue = Color(0xFF8FD8FF)
private val BotBladeHotPink = Color(0xFFFF3EA5)
private val BotBladeOnSurface = Color(0xFFEEF7FF)
private val BotBladeMuted = Color(0xFFAAB8CC)
private val BotBladeDanger = Color(0xFFFF6B8A)

private val BotBladeColors = darkColorScheme(
    primary = BotBladeBabyBlue,
    onPrimary = BotBladeBlack,
    primaryContainer = BotBladeRaised,
    onPrimaryContainer = BotBladeOnSurface,
    secondary = BotBladeHotPink,
    onSecondary = BotBladeBlack,
    secondaryContainer = BotBladePanel,
    onSecondaryContainer = BotBladeOnSurface,
    tertiary = Color(0xFFB89CFF),
    onTertiary = BotBladeBlack,
    background = BotBladeBlack,
    onBackground = BotBladeOnSurface,
    surface = BotBladeBlack,
    onSurface = BotBladeOnSurface,
    surfaceVariant = BotBladePanel,
    onSurfaceVariant = BotBladeMuted,
    inverseSurface = BotBladeOnSurface,
    inverseOnSurface = BotBladeBlack,
    error = BotBladeDanger,
    onError = BotBladeBlack,
)

@Composable
fun BotBladeTheme(
    darkTheme: Boolean = true,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = BotBladeColors, typography = AppTypography, content = content)
}