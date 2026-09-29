package com.subhrodip.squarewise.accounts.auth.abuse

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

/** Verifies Redis rate-limit adapter key, TTL, result, and fail-closed handling. */
class RedisRateLimitBucketStoreTest {

    private val now = Instant.parse("2026-09-29T00:00:10Z")
    private val windowStart = Instant.parse("2026-09-29T00:00:00Z")
    private val cooldown = Instant.parse("2026-09-29T00:00:05Z")

    /** Passes the hex key and bounded window TTL to the atomic Redis script. */
    @Test
    fun `executes atomic decision with hex key and window ttl`() {
        val redis = mock(StringRedisTemplate::class.java)
        var arguments: Array<Any?>? = null
        var keys: List<String>? = null
        doAnswer { invocation ->
            keys = invocation.getArgument(1)
            arguments = invocation.arguments.drop(2).toTypedArray()
            1L
        }.`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(5) { any() }
        )

        val result = RedisRateLimitBucketStore(redis).acquireAtomically(
            key = byteArrayOf(0x01, 0x2a, 0xff.toByte()),
            now = now,
            windowStart = windowStart,
            cooldownCutoff = cooldown,
            maximumRequests = 4
        )

        assertEquals(1, result)
        assertEquals(listOf("squarewise:rate-limit:v1:012aff"), keys)
        assertEquals(
            listOf(
                now.toEpochMilli().toString(),
                windowStart.toEpochMilli().toString(),
                cooldown.toEpochMilli().toString(),
                "4",
                "10"
            ),
            arguments?.map { it.toString() }
        )
    }

    /** Maps a null Redis decision to an unavailable-store error rather than allow. */
    @Test
    fun `fails closed when Redis returns no decision`() {
        val redis = mock(StringRedisTemplate::class.java)
        doAnswer { null }.`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(5) { any() }
        )

        val error = assertThrows(RateLimitStoreUnavailableException::class.java) {
            RedisRateLimitBucketStore(redis).acquireAtomically(
                byteArrayOf(1), now, windowStart, cooldown, 4
            )
        }

        assertInstanceOf(IllegalStateException::class.java, error.cause)
    }

    /** Wraps a Redis exception so callers retain the fail-closed contract. */
    @Test
    fun `wraps Redis failures`() {
        val redis = mock(StringRedisTemplate::class.java)
        doThrow(IllegalStateException("redis down")).`when`(redis).execute(
            any(DefaultRedisScript::class.java),
            anyList(),
            *Array(5) { any() }
        )

        val error = assertThrows(RateLimitStoreUnavailableException::class.java) {
            RedisRateLimitBucketStore(redis).acquireAtomically(
                byteArrayOf(1), now, windowStart, cooldown, 4
            )
        }

        assertEquals("redis down", error.cause?.message)
    }
}
