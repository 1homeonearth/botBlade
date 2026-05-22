package com.princess.botblade.ui.theme

import android.content.Context

private const val PREFS_NAME = "botblade_prefs"
private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"

fun isDynamicColorEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_USE_DYNAMIC_COLOR, false)
