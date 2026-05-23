// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade  // line 7: executes this statement as part of this file's behavior

import android.app.Application  // line 9: executes this statement as part of this file's behavior
import android.content.Context  // line 10: executes this statement as part of this file's behavior
import com.google.android.material.color.DynamicColors  // line 11: executes this statement as part of this file's behavior

class BotBladeApp : Application() {  // line 13: executes this statement as part of this file's behavior
    override fun onCreate() {  // line 14: executes this statement as part of this file's behavior
        super.onCreate()  // line 15: executes this statement as part of this file's behavior
        val prefs = getSharedPreferences("botblade_prefs", Context.MODE_PRIVATE)  // line 16: executes this statement as part of this file's behavior
        if (prefs.getBoolean("use_dynamic_color", false)) {  // line 17: executes this statement as part of this file's behavior
            DynamicColors.applyToActivitiesIfAvailable(this)  // line 18: executes this statement as part of this file's behavior
        }  // line 19: executes this statement as part of this file's behavior
        StartupDiagnostics.install(this, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE, BuildConfig.GIT_SHA)  // line 20: executes this statement as part of this file's behavior
        StartupDiagnostics.mark("di_init")  // line 21: executes this statement as part of this file's behavior
        StartupDiagnostics.mark("application_on_create_end")  // line 22: executes this statement as part of this file's behavior
    }  // line 23: executes this statement as part of this file's behavior
}  // line 24: executes this statement as part of this file's behavior
