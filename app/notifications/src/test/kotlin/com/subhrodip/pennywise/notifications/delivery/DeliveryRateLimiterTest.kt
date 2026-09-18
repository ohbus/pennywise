package com.subhrodip.pennywise.notifications

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class DeliveryRateLimiterTest {
    @Test
    fun `limits recipient within window and resets`() {
        var now = Instant.EPOCH
        val limiter = DeliveryRateLimiter(2, Duration.ofMinutes(1)) { now }
        assertTrue(limiter.allow("alice")); assertTrue(limiter.allow("alice")); assertFalse(limiter.allow("alice"))
        now = now.plusSeconds(61)
        assertTrue(limiter.allow("alice"))
    }
}
