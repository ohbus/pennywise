package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/** Test-profile-only limiter; never registered in local, staging, or production. */
@Component
@Profile("test")
@Primary
class TestRateLimitBucketStore : RateLimitBucketStore {
    private data class Bucket(var started: Instant, var count: Int, var last: Instant)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun acquireAtomically(
        key: ByteArray,
        now: Instant,
        windowStart: Instant,
        cooldownCutoff: Instant,
        maximumRequests: Int
    ): Int {
        val bucketKey = key.joinToString("") { "%02x".format(it) }
        var allowed = 0
        buckets.compute(bucketKey) { _, current ->
            if (current == null || current.started <= windowStart) {
                allowed = 1
                Bucket(now, 1, now)
            } else if (current.count < maximumRequests && current.last <= cooldownCutoff) {
                allowed = 1
                current.copy(count = current.count + 1, last = now)
            } else {
                current
            }
        }
        return allowed
    }
}
