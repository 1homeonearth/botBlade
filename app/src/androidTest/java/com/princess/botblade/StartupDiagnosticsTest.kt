// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade  // line 7: executes this statement as part of this file's behavior

import androidx.test.core.app.ApplicationProvider  // line 9: executes this statement as part of this file's behavior
import androidx.test.ext.junit.runners.AndroidJUnit4  // line 10: executes this statement as part of this file's behavior
import org.junit.Assert.assertTrue  // line 11: executes this statement as part of this file's behavior
import org.junit.Test  // line 12: executes this statement as part of this file's behavior
import org.junit.runner.RunWith  // line 13: executes this statement as part of this file's behavior

@RunWith(AndroidJUnit4::class)  // line 15: executes this statement as part of this file's behavior
class StartupDiagnosticsTest {  // line 16: executes this statement as part of this file's behavior
    @Test  // line 17: executes this statement as part of this file's behavior
    fun writesArtifactOnStartupExceptionSimulation() {  // line 18: executes this statement as part of this file's behavior
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()  // line 19: executes this statement as part of this file's behavior
        StartupDiagnostics.writeArtifactForTest(app, IllegalStateException("token=abc.def.ghijklmnopqrstuv"))  // line 20: executes this statement as part of this file's behavior
        val artifact = StartupDiagnostics.readLatest(app)  // line 21: executes this statement as part of this file's behavior
        assertTrue(artifact?.contains("exceptionClass") == true)  // line 22: executes this statement as part of this file's behavior
        assertTrue(artifact?.contains("[REDACTED]") == true)  // line 23: executes this statement as part of this file's behavior
    }  // line 24: executes this statement as part of this file's behavior
}  // line 25: executes this statement as part of this file's behavior
