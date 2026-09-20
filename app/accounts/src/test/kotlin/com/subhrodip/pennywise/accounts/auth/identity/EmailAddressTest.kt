package com.subhrodip.pennywise.accounts.auth.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies canonical email input rules used by the authentication boundary. */
class EmailAddressTest {
    @Test
    fun `trims and case folds address`() {
        assertEquals("alice@example.com", EmailAddress.parse("  Alice@EXAMPLE.COM ").value)
    }

    @Test
    fun `canonicalizes internationalized domain`() {
        assertEquals("alice@xn--bcher-kva.example", EmailAddress.parse("Alice@bücher.example").value)
    }

    @Test
    fun `rejects malformed addresses`() {
        listOf("", "alice", "@example.com", "alice@", "a@@example.com", "alice @example.com")
            .forEach { raw -> assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) } }
    }

    @Test
    fun `rejects overlong local part`() {
        val raw = "a".repeat(65) + "@example.com"
        assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) }
    }

    @Test
    fun `rejects overlong address`() {
        val raw = "a".repeat(240) + "@example.com"
        assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) }
    }
}
