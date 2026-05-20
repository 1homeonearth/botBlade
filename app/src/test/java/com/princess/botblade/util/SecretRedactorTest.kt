package com.princess.botblade.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SecretRedactorTest {
    @Test
    fun redactRedactsAssignmentValuesContainingSpaces() {
        val redacted = SecretRedactor.redact("Authorization: Bearer abc123.super.secret")

        assertEquals("Authorization=[REDACTED]", redacted)
    }
}
