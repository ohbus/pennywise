package com.subhrodip.pennywise.notifications.delivery.rate

import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

/** Contract for distributed notification delivery admission decisions. */
interface DeliveryRateLimiter {
    /** Returns whether the subject remains within the configured delivery limit. */
    fun allow(subject: String): Boolean
}

/** Redis-backed notification delivery limiter; Redis failures propagate and fail closed. */
class RedisDeliveryRateLimiter(private val redis: StringRedisTemplate, private val limit: Int = 10, private val window: Duration = Duration.ofMinutes(1)) : DeliveryRateLimiter {
    private val redisScript = DefaultRedisScript<Long>(
        "local count = redis.call('INCR', KEYS[1]); if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; if count <= tonumber(ARGV[2]) then return 1 else return 0 end",
        Long::class.java
    )

    override fun allow(subject: String): Boolean {
        val decision = redis.execute(redisScript, listOf("pennywise:notification-rate:v1:$subject"), window.seconds.toString(), limit.toString())
            ?: throw IllegalStateException("Redis returned no notification rate-limit decision")
        return decision == 1L
    }
}
