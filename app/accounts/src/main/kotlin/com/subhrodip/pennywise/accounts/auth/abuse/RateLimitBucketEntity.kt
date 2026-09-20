package com.subhrodip.pennywise.accounts.auth.abuse

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Persistent operational throttle state; the digest is not an identity value. */
@Entity
@Table(name = "auth_rate_limit_buckets")
class RateLimitBucketEntity(
    @Id
    @Column(name = "rate_key_digest", nullable = false)
    var rateKeyDigest: ByteArray,
    @Column(name = "window_started_at", nullable = false)
    var windowStartedAt: Instant,
    @Column(name = "request_count", nullable = false)
    var requestCount: Int,
    @Column(name = "last_requested_at")
    var lastRequestedAt: Instant? = null
)
