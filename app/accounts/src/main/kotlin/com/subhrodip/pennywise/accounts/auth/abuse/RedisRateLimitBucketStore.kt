package com.subhrodip.pennywise.accounts.auth.abuse

import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile

/** Redis-backed atomic login limiter; Redis is the only runtime rate-limit store. */
@Component
@Profile("!test")
class RedisRateLimitBucketStore(
    private val redis: StringRedisTemplate
) : RateLimitBucketStore {
    private val script = DefaultRedisScript<Long>(SCRIPT, Long::class.java)

    override fun acquireAtomically(
        key: ByteArray,
        now: Instant,
        windowStart: Instant,
        cooldownCutoff: Instant,
        maximumRequests: Int
    ): Int {
        val redisKey = "pennywise:rate-limit:v1:${key.toHex()}"
        return try {
            redis.execute(
                script,
                listOf(redisKey),
                now.toEpochMilli().toString(),
                windowStart.toEpochMilli().toString(),
                cooldownCutoff.toEpochMilli().toString(),
                maximumRequests.toString(),
                Duration.between(windowStart, now).toSeconds().coerceAtLeast(1).toString()
            )?.toInt() ?: throw IllegalStateException("Redis returned no rate-limit decision")
        } catch (exception: Exception) {
            throw RateLimitStoreUnavailableException(exception)
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val SCRIPT = """
            local now = tonumber(ARGV[1])
            local window_start = tonumber(ARGV[2])
            local cooldown = tonumber(ARGV[3])
            local maximum = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])
            local count = tonumber(redis.call('HGET', KEYS[1], 'count') or '0')
            local started = tonumber(redis.call('HGET', KEYS[1], 'started') or '0')
            local last = tonumber(redis.call('HGET', KEYS[1], 'last') or '0')
            if started == 0 or started <= window_start then
              redis.call('HSET', KEYS[1], 'count', 1, 'started', now, 'last', now)
              redis.call('EXPIRE', KEYS[1], ttl)
              return 1
            end
            if count < maximum and last <= cooldown then
              redis.call('HINCRBY', KEYS[1], 'count', 1)
              redis.call('HSET', KEYS[1], 'last', now)
              redis.call('EXPIRE', KEYS[1], ttl)
              return 1
            end
            return 0
        """
    }
}
