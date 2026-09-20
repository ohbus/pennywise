package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Duration
import java.time.Instant

/** Provider-neutral anti-abuse policy for passwordless login requests. */
class LoginAbusePolicy(
    private val window: Duration = Duration.ofMinutes(15),
    private val maximumRequests: Int = 5,
    private val resendCooldown: Duration = Duration.ofSeconds(60)
) {
    init {
        require(!window.isZero && !window.isNegative) { "Rate-limit window must be positive" }
        require(maximumRequests > 0) { "Maximum requests must be positive" }
        require(!resendCooldown.isNegative) { "Resend cooldown cannot be negative" }
        require(resendCooldown <= window) { "Resend cooldown cannot exceed the window" }
    }

    /**
     * Evaluates a request without revealing whether an account exists.
     *
     * @param now current clock value.
     * @param state atomically read state for the canonical abuse key.
     * @return generic decision and retry time; callers must use an atomic store update.
     */
    fun evaluate(now: Instant, state: LoginRateLimitState): LoginRateLimitDecision {
        if (state.windowStartedAt.plus(window).isBefore(now)) {
            return LoginRateLimitDecision.ALLOW
        }
        if (state.requestCount >= maximumRequests) {
            return LoginRateLimitDecision.DENY
        }
        if (state.lastRequestedAt != null && state.lastRequestedAt.plus(resendCooldown).isAfter(now)) {
            return LoginRateLimitDecision.DENY
        }
        return LoginRateLimitDecision.ALLOW
    }
}

/** State atomically stored per canonical email/network abuse key. */
data class LoginRateLimitState(
    val windowStartedAt: Instant,
    val requestCount: Int,
    val lastRequestedAt: Instant?
)

/** Generic rate-limit result that does not encode account existence. */
enum class LoginRateLimitDecision { ALLOW, DENY }
