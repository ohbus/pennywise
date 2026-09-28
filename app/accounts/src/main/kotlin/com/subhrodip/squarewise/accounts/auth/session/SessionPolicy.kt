package com.subhrodip.squarewise.accounts.auth.session

import java.time.Duration
import java.time.Instant

/**
 * Pure policy for bounded refresh-token session continuity.
 *
 * A successful refresh extends only the idle boundary. The absolute boundary is
 * fixed at session creation, which prevents an active client from creating an
 * indefinitely sliding session. The policy has no persistence, HTTP, cache, or
 * identity-provider dependency so its boundary behavior can be tested directly.
 *
 * @param accessTokenLifetime Short lifetime of an issued access token.
 * @param refreshIdleLifetime Maximum time a refresh session may remain unused.
 * @param absoluteSessionLifetime Maximum lifetime from initial session creation.
 * @param clockSkew Tolerance used when evaluating a refresh boundary.
 */
class SessionPolicy(
    val accessTokenLifetime: Duration,
    val refreshIdleLifetime: Duration,
    val absoluteSessionLifetime: Duration,
    val clockSkew: Duration = Duration.ZERO
) {
    init {
        require(!accessTokenLifetime.isZero && !accessTokenLifetime.isNegative) {
            "Access-token lifetime must be positive"
        }
        require(!refreshIdleLifetime.isZero && !refreshIdleLifetime.isNegative) {
            "Refresh idle lifetime must be positive"
        }
        require(!absoluteSessionLifetime.isZero && !absoluteSessionLifetime.isNegative) {
            "Absolute session lifetime must be positive"
        }
        require(accessTokenLifetime < refreshIdleLifetime) {
            "Access-token lifetime must be shorter than refresh idle lifetime"
        }
        require(refreshIdleLifetime <= absoluteSessionLifetime) {
            "Refresh idle lifetime must not exceed absolute session lifetime"
        }
        require(!clockSkew.isNegative) { "Clock skew must not be negative" }
    }

    /**
     * Calculates the initial idle and immutable absolute boundaries.
     *
     * @param createdAt Session creation timestamp.
     * @return Initial session expiry boundaries.
     */
    fun initialExpiry(createdAt: Instant): SessionExpiry {
        val absoluteExpiry = createdAt.plus(absoluteSessionLifetime)
        return SessionExpiry(
            idleExpiresAt = min(createdAt.plus(refreshIdleLifetime), absoluteExpiry),
            absoluteExpiresAt = absoluteExpiry
        )
    }

    /**
     * Calculates the next idle boundary after a successful refresh.
     *
     * @param refreshedAt Successful refresh timestamp.
     * @param absoluteExpiresAt Immutable session-family boundary.
     * @return New expiry boundaries preserving the absolute deadline.
     */
    fun refreshedExpiry(refreshedAt: Instant, absoluteExpiresAt: Instant): SessionExpiry =
        SessionExpiry(
            idleExpiresAt = min(refreshedAt.plus(refreshIdleLifetime), absoluteExpiresAt),
            absoluteExpiresAt = absoluteExpiresAt
        )

    /**
     * Determines whether refresh is still permitted at a timestamp.
     *
     * The configured skew is applied conservatively: a session is treated as
     * expired once the current time plus skew reaches either boundary.
     *
     * @param expiry Session boundaries.
     * @param now Current server timestamp.
     * @return `true` when idle and absolute boundaries have not elapsed.
     */
    fun isExpired(expiry: SessionExpiry, now: Instant): Boolean {
        val comparisonTime = now.plus(clockSkew)
        return !comparisonTime.isBefore(expiry.idleExpiresAt) ||
            !comparisonTime.isBefore(expiry.absoluteExpiresAt)
    }

    private fun min(first: Instant, second: Instant): Instant =
        if (first.isBefore(second)) first else second
}
