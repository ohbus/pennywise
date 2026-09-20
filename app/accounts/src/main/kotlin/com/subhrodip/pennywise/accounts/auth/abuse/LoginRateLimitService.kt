package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Duration
import java.time.Instant
import org.springframework.transaction.annotation.Transactional

/** Application service for atomic passwordless login request throttling. */
class LoginRateLimitService(
    private val keyDeriver: LoginRateLimitKeyDeriver,
    private val repository: RateLimitBucketRepository,
    private val window: Duration = Duration.ofMinutes(15),
    private val maximumRequests: Int = 5,
    private val resendCooldown: Duration = Duration.ofSeconds(60)
) {
    init {
        LoginAbusePolicy(window, maximumRequests, resendCooldown)
    }

    /**
     * Atomically acquires one request slot for a canonical email/network pair.
     *
     * @return true when allowed; false is a generic denial.
     */
    @Transactional
    fun tryAcquire(email: String, networkPartition: String, now: Instant): Boolean {
        val key = keyDeriver.derive(email, networkPartition)
        val updated = repository.acquireAtomically(
            key = key,
            now = now,
            windowStart = now.minus(window),
            cooldownCutoff = now.minus(resendCooldown),
            maximumRequests = maximumRequests
        )
        return updated == 1
    }
}
