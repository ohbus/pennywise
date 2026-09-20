package com.subhrodip.pennywise.accounts.auth.abuse

import com.subhrodip.pennywise.accounts.auth.credential.HmacCredentialDigest
import java.util.Arrays
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies opaque, canonical, partitioned rate-limit key derivation. */
class LoginRateLimitKeyDeriverTest {
    private val deriver = LoginRateLimitKeyDeriver(HmacCredentialDigest(ByteArray(32) { it.toByte() }))

    @Test
    fun `canonical email produces the same opaque key`() {
        assertTrue(Arrays.equals(deriver.derive(" Alice@EXAMPLE.COM ", "net-a"), deriver.derive("alice@example.com", "net-a")))
    }

    @Test
    fun `different network partitions produce different keys`() {
        assertFalse(Arrays.equals(deriver.derive("alice@example.com", "net-a"), deriver.derive("alice@example.com", "net-b")))
    }

    @Test
    fun `rejects invalid partition`() {
        assertThrows(IllegalArgumentException::class.java) { deriver.derive("alice@example.com", " ") }
        assertThrows(IllegalArgumentException::class.java) { deriver.derive("alice@example.com", "x y") }
    }
}
