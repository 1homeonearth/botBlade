// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.theme  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior

private const val PREFS_NAME = "botblade_prefs"  // line 11: executes this statement as part of this file's behavior
private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"  // line 12: executes this statement as part of this file's behavior

fun isDynamicColorEnabled(context: Context): Boolean =  // line 14: executes this statement as part of this file's behavior
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)  // line 15: executes this statement as part of this file's behavior
        .getBoolean(KEY_USE_DYNAMIC_COLOR, false)  // line 16: executes this statement as part of this file's behavior
