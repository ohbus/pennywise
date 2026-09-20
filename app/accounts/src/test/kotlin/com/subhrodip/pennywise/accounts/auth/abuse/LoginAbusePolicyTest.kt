package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies bounded cooldown and request-window behavior for login abuse policy. */
class LoginAbusePolicyTest {
    private val now = Instant.parse("2026-09-20T00:00:00Z")
    private val policy = LoginAbusePolicy()

    @Test
    fun `allows first request`() {
        assertEquals(LoginRateLimitDecision.ALLOW, policy.evaluate(now, state(0, null)))
    }

    @Test
    fun `denies resend during cooldown`() {
        assertEquals(LoginRateLimitDecision.DENY, policy.evaluate(now, state(1, now.minusSeconds(30))))
    }

    @Test
    fun `denies exhausted request window`() {
        assertEquals(LoginRateLimitDecision.DENY, policy.evaluate(now, state(5, now.minusSeconds(120))))
    }

    @Test
    fun `resets after window`() {
        assertEquals(
            LoginRateLimitDecision.ALLOW,
            policy.evaluate(now, LoginRateLimitState(now.minusSeconds(901), 5, now.minusSeconds(901)))
        )
    }

    @Test
    fun `rejects invalid policy configuration`() {
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(maximumRequests = 0) }
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(resendCooldown = java.time.Duration.ofMinutes(16)) }
    }

    private fun state(count: Int, lastRequestedAt: Instant?) = LoginRateLimitState(
        windowStartedAt = now.minusSeconds(120), requestCount = count, lastRequestedAt = lastRequestedAt
    )
}
