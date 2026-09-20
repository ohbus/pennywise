package com.subhrodip.pennywise.accounts.auth.abuse

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** Port for durable, conditional login-throttle state transitions. */
interface RateLimitBucketRepository : JpaRepository<RateLimitBucketEntity, ByteArray> {
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
