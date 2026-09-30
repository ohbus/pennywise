package com.subhrodip.squarewise.accounts.auth.abuse

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
    fun `allows a resend at the exact cooldown boundary`() {
        assertEquals(LoginRateLimitDecision.ALLOW, policy.evaluate(now, state(1, now.minusSeconds(60))))
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
    fun `resets at the exact window boundary`() {
        assertEquals(
            LoginRateLimitDecision.ALLOW,
            policy.evaluate(now, LoginRateLimitState(now.minusSeconds(900), 5, now.minusSeconds(900)))
        )
    }

    @Test
    fun `rejects invalid policy configuration`() {
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(window = java.time.Duration.ZERO) }
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(window = java.time.Duration.ofSeconds(-1)) }
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(maximumRequests = 0) }
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(resendCooldown = java.time.Duration.ofSeconds(-1)) }
        assertThrows(IllegalArgumentException::class.java) { LoginAbusePolicy(resendCooldown = java.time.Duration.ofMinutes(16)) }
    }

    private fun state(count: Int, lastRequestedAt: Instant?) = LoginRateLimitState(
        windowStartedAt = now.minusSeconds(120), requestCount = count, lastRequestedAt = lastRequestedAt
    )
}
