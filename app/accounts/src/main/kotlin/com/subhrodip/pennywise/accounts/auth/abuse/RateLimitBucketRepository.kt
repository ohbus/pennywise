package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** Port for durable, conditional login-throttle state transitions. */
interface RateLimitBucketRepository : JpaRepository<RateLimitBucketEntity, ByteArray> {
    /** Atomically creates or increments a bucket; returns zero when denied. */
    @Modifying
    @Query(
        value = """
            INSERT INTO auth_rate_limit_buckets
                (rate_key_digest, window_started_at, request_count, last_requested_at)
            VALUES (:key, :now, 1, :now)
            ON CONFLICT (rate_key_digest) DO UPDATE
            SET window_started_at = CASE
                    WHEN auth_rate_limit_buckets.window_started_at <= :windowStart THEN :now
                    ELSE auth_rate_limit_buckets.window_started_at END,
                request_count = CASE
                    WHEN auth_rate_limit_buckets.window_started_at <= :windowStart THEN 1
                    ELSE auth_rate_limit_buckets.request_count + 1 END,
                last_requested_at = :now
            WHERE auth_rate_limit_buckets.window_started_at <= :windowStart
               OR (
                    auth_rate_limit_buckets.request_count < :maximumRequests
                    AND (auth_rate_limit_buckets.last_requested_at IS NULL
                         OR auth_rate_limit_buckets.last_requested_at <= :cooldownCutoff)
               )
            """,
        nativeQuery = true
    )
    fun acquireAtomically(
        @Param("key") key: ByteArray,
        @Param("now") now: Instant,
        @Param("windowStart") windowStart: Instant,
        @Param("cooldownCutoff") cooldownCutoff: Instant,
        @Param("maximumRequests") maximumRequests: Int
    ): Int

    /**
     * Records an allowed request only when cooldown and window policy permit it.
     * The service must first evaluate the returned row as a generic decision.
     */
    @Modifying
    @Query(
        """
        UPDATE RateLimitBucketEntity b
           SET b.requestCount = b.requestCount + 1, b.lastRequestedAt = :now
         WHERE b.rateKeyDigest = :key
           AND b.windowStartedAt > :windowStart
           AND b.requestCount < :maximumRequests
           AND (b.lastRequestedAt IS NULL OR b.lastRequestedAt <= :cooldownCutoff)
        """
    )
    fun incrementIfAllowed(
        @Param("key") key: ByteArray,
        @Param("now") now: Instant,
        @Param("windowStart") windowStart: Instant,
        @Param("cooldownCutoff") cooldownCutoff: Instant,
        @Param("maximumRequests") maximumRequests: Int
    ): Int
}
