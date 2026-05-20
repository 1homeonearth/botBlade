package com.princess.botblade

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupGuardTest {
    @Test
    fun entersSafeModeAfterConsecutiveEarlyCrashes() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val guard = StartupGuard(app)
        guard.resetIntegrationState()
        repeat(3) { guard.recordEarlyCrash("startup_step") }
        assertTrue(guard.isSafeModeEnabled())
    }

    @Test
    fun exitsSafeModeAfterStartupComplete() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val guard = StartupGuard(app)
        repeat(3) { guard.recordEarlyCrash("startup_step") }
        assertTrue(guard.isSafeModeEnabled())
        guard.recordStartupSuccess()
        assertFalse(guard.isSafeModeEnabled())
    }
}
