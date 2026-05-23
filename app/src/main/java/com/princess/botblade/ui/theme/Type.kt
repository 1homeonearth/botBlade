// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.theme  // line 7: executes this statement as part of this file's behavior

import androidx.compose.material3.Typography  // line 9: executes this statement as part of this file's behavior
import androidx.compose.ui.text.TextStyle  // line 10: executes this statement as part of this file's behavior
import androidx.compose.ui.text.font.FontWeight  // line 11: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.sp  // line 12: executes this statement as part of this file's behavior

val AppTypography = Typography(  // line 14: executes this statement as part of this file's behavior
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),  // line 15: executes this statement as part of this file's behavior
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),  // line 16: executes this statement as part of this file's behavior
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),  // line 17: executes this statement as part of this file's behavior
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),  // line 18: executes this statement as part of this file's behavior
)  // line 19: executes this statement as part of this file's behavior
