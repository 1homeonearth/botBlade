package com.princess.botblade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupGuardTest {
    @Test
    fun `crash loop increments attempts when previous startup incomplete`() {
        assertEquals(1, StartupGuard.computeNextAttemptCount(lastStartupSucceeded = false, earlyCrashAttempts = 0))
        assertEquals(3, StartupGuard.computeNextAttemptCount(lastStartupSucceeded = false, earlyCrashAttempts = 2))
    }

    @Test
    fun `successful startup resets early crash counter`() {
        assertEquals(0, StartupGuard.computeNextAttemptCount(lastStartupSucceeded = true, earlyCrashAttempts = 5))
    }

    @Test
    fun `safe mode toggles at threshold`() {
        assertFalse(StartupGuard.shouldEnableSafeMode(2))
        assertTrue(StartupGuard.shouldEnableSafeMode(3))
    }
}
