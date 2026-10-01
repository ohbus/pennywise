package com.subhrodip.squarewise.accounts.auth.identity

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

    @Test
    fun `rejects invalid domain labels and control characters`() {
        listOf(
            "alice@.example.com",
            "alice@example..com",
            "alice@${"a".repeat(64)}.com",
            "alice\u0000@example.com",
            "alice@example.com\u0000",
            "alice@exa mple.com",
        ).forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) }
        }
    }

    @Test
    fun `rejects an invalid internationalized domain sequence`() {
        assertThrows(IllegalArgumentException::class.java) {
            EmailAddress.parse("alice@\uD800.example")
        }
    }

    @Test
    fun `rejects an address exceeding the complete length limit`() {
        val raw = "a@${"d".repeat(250)}.com"

        assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) }
    }

    @Test
    fun `accepts the maximum local and complete address lengths`() {
        val local = "l".repeat(64)
        val domain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(61)

        assertEquals("$local@$domain", EmailAddress.parse("$local@$domain").value)
        assertEquals(254, "$local@$domain".length)
    }

    @Test
    fun `rejects domain labels that violate IDN separator rules`() {
        listOf("alice@-example.com", "alice@example-.com", "alice@.com.").forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) { EmailAddress.parse(raw) }
        }
    }
}
