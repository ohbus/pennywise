package com.subhrodip.squarewise.notifications.delivery.rate

import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

/** Verifies the Redis limiter's atomic key, window arguments, and fail-closed outcomes. */
class RedisDeliveryRateLimiterTest {
    @Test
    fun `executes atomic decision with subject key window ttl and limit`() {
        val redis = mock(StringRedisTemplate::class.java)
        var keys: List<String>? = null
        var arguments: Array<Any?>? = null
        doAnswer { invocation ->
            keys = invocation.getArgument(1)
            arguments = invocation.arguments.drop(2).toTypedArray()
            1L
        }.`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(2) { any() }
        )

        val result = RedisDeliveryRateLimiter(redis, limit = 3, window = Duration.ofSeconds(45))
            .allow("alice@example.com")

        assertEquals(true, result)
        assertEquals(listOf("squarewise:notification-rate:v1:alice@example.com"), keys)
        assertEquals(listOf("45", "3"), arguments?.map { it.toString() })
    }

    @Test
    fun `maps atomic deny decision to false`() {
        val redis = mock(StringRedisTemplate::class.java)
        doAnswer { 0L }.`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(2) { any() }
        )

        assertEquals(false, RedisDeliveryRateLimiter(redis, limit = 1).allow("alice@example.com"))
    }

    @Test
    fun `fails closed when Redis returns no decision`() {
        val redis = mock(StringRedisTemplate::class.java)
        doAnswer { null }.`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(2) { any() }
        )

        val error = assertThrows(IllegalStateException::class.java) {
            RedisDeliveryRateLimiter(redis).allow("alice@example.com")
        }

        assertEquals("Redis returned no notification rate-limit decision", error.message)
    }

    @Test
    fun `propagates Redis failure so callers cannot fail open`() {
        val redis = mock(StringRedisTemplate::class.java)
        doThrow(IllegalStateException("redis down")).`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(2) { any() }
        )

        val error = assertThrows(IllegalStateException::class.java) {
            RedisDeliveryRateLimiter(redis).allow("alice@example.com")
        }

        assertEquals("redis down", error.message)
    }
}
