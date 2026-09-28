package com.subhrodip.squarewise.bff.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies exact-origin browser policy behavior without a network dependency. */
class BrowserOriginPolicyTest {
    private val policy = BrowserOriginPolicy(listOf("https://app.example.test", "http://localhost:3000"))

    @Test
    fun `accepts native requests without an origin`() {
        assertTrue(policy.allows(null))
    }

    @Test
    fun `accepts only exact configured origins`() {
        assertTrue(policy.allows("https://app.example.test"))
        assertTrue(policy.allows("HTTP://LOCALHOST:3000"))
        assertFalse(policy.allows("https://evil.example.test"))
        assertFalse(policy.allows("https://app.example.test/path"))
    }

    @Test
    fun `rejects wildcard configuration`() {
        assertThrows(IllegalArgumentException::class.java) {
            BrowserOriginPolicy(listOf("*"))
        }
    }
}
