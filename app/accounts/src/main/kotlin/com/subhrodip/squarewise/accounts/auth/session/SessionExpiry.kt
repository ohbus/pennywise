package com.subhrodip.squarewise.accounts.auth.session

import java.time.Instant

/**
 * Effective expiry boundaries for one refresh-token session.
 *
 * The idle boundary may move after a successful refresh, while the absolute
 * boundary is immutable for the lifetime of the session family.
 *
 * @property idleExpiresAt Latest timestamp at which an unused session may be refreshed.
 * @property absoluteExpiresAt Hard upper bound that refresh can never extend.
 */
data class SessionExpiry(
    val idleExpiresAt: Instant,
    val absoluteExpiresAt: Instant
) {
    init {
        require(idleExpiresAt <= absoluteExpiresAt) {
            "Idle expiry must not be later than absolute expiry"
        }
    }
}
