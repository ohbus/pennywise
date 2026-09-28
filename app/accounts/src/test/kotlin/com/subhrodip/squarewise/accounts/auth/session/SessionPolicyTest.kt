package com.subhrodip.squarewise.accounts.auth.session

import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies the externally observable refresh-session expiry policy. */
class SessionPolicyTest {
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val policy = SessionPolicy(
        accessTokenLifetime = Duration.ofMinutes(10),
        refreshIdleLifetime = Duration.ofDays(30),
        absoluteSessionLifetime = Duration.ofDays(90),
        clockSkew = Duration.ZERO
    )

    @Test
    fun `initial expiry creates a thirty day idle and ninety day absolute boundary`() {
        val expiry = policy.initialExpiry(createdAt)

        assertEquals(createdAt.plus(Duration.ofDays(30)), expiry.idleExpiresAt)
        assertEquals(createdAt.plus(Duration.ofDays(90)), expiry.absoluteExpiresAt)
    }

    @Test
    fun `refresh moves only the idle boundary`() {
        val initial = policy.initialExpiry(createdAt)
        val refreshedAt = createdAt.plus(Duration.ofDays(20))

        val refreshed = policy.refreshedExpiry(refreshedAt, initial.absoluteExpiresAt)

        assertEquals(refreshedAt.plus(Duration.ofDays(30)), refreshed.idleExpiresAt)
        assertEquals(initial.absoluteExpiresAt, refreshed.absoluteExpiresAt)
    }

    @Test
    fun `refresh idle boundary is capped by absolute expiry`() {
        val initial = policy.initialExpiry(createdAt)
        val refreshed = policy.refreshedExpiry(
            createdAt.plus(Duration.ofDays(80)),
            initial.absoluteExpiresAt
        )

        assertEquals(initial.absoluteExpiresAt, refreshed.idleExpiresAt)
        assertEquals(initial.absoluteExpiresAt, refreshed.absoluteExpiresAt)
    }

    @Test
    fun `session is valid immediately before each applicable boundary`() {
        val initial = policy.initialExpiry(createdAt)
        val refreshed = policy.refreshedExpiry(
            createdAt.plus(Duration.ofDays(60)),
            initial.absoluteExpiresAt
        )

        assertFalse(policy.isExpired(initial, initial.idleExpiresAt.minusNanos(1)))
        assertFalse(policy.isExpired(refreshed, refreshed.absoluteExpiresAt.minusNanos(1)))
    }

    @Test
    fun `session expires at idle boundary`() {
        val expiry = policy.initialExpiry(createdAt)

        assertTrue(policy.isExpired(expiry, expiry.idleExpiresAt))
    }

    @Test
    fun `session expires at absolute boundary even after activity`() {
        val initial = policy.initialExpiry(createdAt)
        val refreshed = policy.refreshedExpiry(
            createdAt.plus(Duration.ofDays(80)),
            initial.absoluteExpiresAt
        )

        assertTrue(policy.isExpired(refreshed, initial.absoluteExpiresAt))
    }

    @Test
    fun `clock skew expires a session conservatively`() {
        val skewedPolicy = SessionPolicy(
            accessTokenLifetime = Duration.ofMinutes(10),
            refreshIdleLifetime = Duration.ofDays(30),
            absoluteSessionLifetime = Duration.ofDays(90),
            clockSkew = Duration.ofSeconds(30)
        )
        val expiry = skewedPolicy.initialExpiry(createdAt)

        assertTrue(skewedPolicy.isExpired(expiry, expiry.idleExpiresAt.minusSeconds(29)))
        assertFalse(skewedPolicy.isExpired(expiry, expiry.idleExpiresAt.minusSeconds(31)))
    }

    @Test
    fun `invalid policy values fail fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ZERO, Duration.ofDays(30), Duration.ofDays(90))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofMinutes(10), Duration.ofDays(90), Duration.ofDays(30))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SessionPolicy(Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(30))
        }
    }
}
