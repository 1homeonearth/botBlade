package com.princess.royalscepter.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SecretRedactorTest {
    @Test
    fun redactHidesNonBlankValuesAndLeavesEmptyValuesBlank() {
        assertEquals("••••••••", SecretRedactor.redact("super-secret-token"))
        assertEquals("", SecretRedactor.redact(""))
        assertEquals("", SecretRedactor.redact(null))
    }

    @Test
    fun redactMapOnlyRedactsSensitiveKeys() {
        val redacted = SecretRedactor.redactMap(
            mapOf(
                "discordToken" to "abc123",
                "api_key" to "key-value",
                "displayName" to "Princess Bot",
                "notes" to null,
            ),
        )

        assertEquals("••••••••", redacted["discordToken"])
        assertEquals("••••••••", redacted["api_key"])
        assertEquals("Princess Bot", redacted["displayName"])
        assertEquals("", redacted["notes"])
    }
}
