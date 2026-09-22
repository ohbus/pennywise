package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant

/** Atomic store boundary for passwordless-login abuse buckets. */
interface RateLimitBucketStore {
    /**
     * Atomically consumes one request slot.
     *
     * @return one when the request is allowed, zero when throttled.
     * @throws RateLimitStoreUnavailableException when the store cannot decide safely.
     */
    fun acquireAtomically(
        key: ByteArray,
        now: Instant,
        windowStart: Instant,
        cooldownCutoff: Instant,
        maximumRequests: Int
    ): Int
}
