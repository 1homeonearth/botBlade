// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.theme  // line 7: executes this statement as part of this file's behavior

import android.os.Build  // line 9: executes this statement as part of this file's behavior
import androidx.compose.foundation.isSystemInDarkTheme  // line 10: executes this statement as part of this file's behavior
import androidx.compose.material3.MaterialTheme  // line 11: executes this statement as part of this file's behavior
import androidx.compose.material3.darkColorScheme  // line 12: executes this statement as part of this file's behavior
import androidx.compose.material3.dynamicDarkColorScheme  // line 13: executes this statement as part of this file's behavior
import androidx.compose.material3.dynamicLightColorScheme  // line 14: executes this statement as part of this file's behavior
import androidx.compose.material3.lightColorScheme  // line 15: executes this statement as part of this file's behavior
import androidx.compose.runtime.Composable  // line 16: executes this statement as part of this file's behavior
import androidx.compose.ui.graphics.Color  // line 17: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.LocalContext  // line 18: executes this statement as part of this file's behavior

private val SeedPurple = Color(0xFF8F5BFF)  // line 20: executes this statement as part of this file's behavior
private val GoldPrimary = Color(0xFFFFD166)  // line 21: executes this statement as part of this file's behavior

private val LightColors = lightColorScheme(  // line 23: executes this statement as part of this file's behavior
    primary = SeedPurple,  // line 24: executes this statement as part of this file's behavior
    secondary = GoldPrimary,  // line 25: executes this statement as part of this file's behavior
    tertiary = Color(0xFF5F5A78),  // line 26: executes this statement as part of this file's behavior
    surface = Color(0xFFF6F2FF),  // line 27: executes this statement as part of this file's behavior
    surfaceVariant = Color(0xFFE8E0F8),  // line 28: executes this statement as part of this file's behavior
)  // line 29: executes this statement as part of this file's behavior

private val DarkColors = darkColorScheme(  // line 31: executes this statement as part of this file's behavior
    primary = SeedPurple,  // line 32: executes this statement as part of this file's behavior
    secondary = GoldPrimary,  // line 33: executes this statement as part of this file's behavior
    tertiary = Color(0xFFCCC2FF),  // line 34: executes this statement as part of this file's behavior
    surface = Color(0xFF0D0A17),  // line 35: executes this statement as part of this file's behavior
    surfaceVariant = Color(0xFF2A2438),  // line 36: executes this statement as part of this file's behavior
)  // line 37: executes this statement as part of this file's behavior

@Composable  // line 39: executes this statement as part of this file's behavior
fun BotBladeTheme(  // line 40: executes this statement as part of this file's behavior
    darkTheme: Boolean = isSystemInDarkTheme(),  // line 41: executes this statement as part of this file's behavior
    useDynamicColor: Boolean = false,  // line 42: executes this statement as part of this file's behavior
    content: @Composable () -> Unit,  // line 43: executes this statement as part of this file's behavior
) {  // line 44: executes this statement as part of this file's behavior
    val context = LocalContext.current  // line 45: executes this statement as part of this file's behavior
    val colorScheme = when {  // line 46: executes this statement as part of this file's behavior
        useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)  // line 47: executes this statement as part of this file's behavior
        useDynamicColor && !darkTheme -> dynamicLightColorScheme(context)  // line 48: executes this statement as part of this file's behavior
        darkTheme -> DarkColors  // line 49: executes this statement as part of this file's behavior
        else -> LightColors  // line 50: executes this statement as part of this file's behavior
    }  // line 51: executes this statement as part of this file's behavior
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)  // line 52: executes this statement as part of this file's behavior
}  // line 53: executes this statement as part of this file's behavior
