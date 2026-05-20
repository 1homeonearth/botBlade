package com.princess.botblade

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupDiagnosticsTest {
    @Test
    fun writesArtifactOnStartupExceptionSimulation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        StartupDiagnostics.writeArtifactForTest(app, IllegalStateException("token=abc.def.ghijklmnopqrstuv"))
        val artifact = StartupDiagnostics.readLatest(app)
        assertTrue(artifact?.contains("exceptionClass") == true)
        assertTrue(artifact?.contains("[REDACTED]") == true)
    }
}
