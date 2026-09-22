package com.subhrodip.pennywise.notifications.delivery

import com.subhrodip.pennywise.notifications.delivery.rate.TestDeliveryRateLimiter
import com.subhrodip.pennywise.notifications.delivery.model.DeliveryChannel
import com.subhrodip.pennywise.notifications.delivery.model.DeliveryOutcome
import com.subhrodip.pennywise.notifications.delivery.model.RetryDecision
import com.subhrodip.pennywise.notifications.delivery.persistence.EventDeduplicator
import com.subhrodip.pennywise.notifications.delivery.persistence.InboxDeduplicator
import com.subhrodip.pennywise.notifications.delivery.policy.DeliveryPolicy
import com.subhrodip.pennywise.notifications.delivery.policy.RetryPolicy
import com.subhrodip.pennywise.notifications.delivery.rate.DeliveryRateLimiter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class DeliveryRateLimiterTest {
    @Test
    fun `limits recipient within window and resets`() {
        var now = Instant.EPOCH
        val limiter = TestDeliveryRateLimiter(2, Duration.ofMinutes(1)) { now }
        assertTrue(limiter.allow("alice")); assertTrue(limiter.allow("alice")); assertFalse(limiter.allow("alice"))
        now = now.plusSeconds(61)
        assertTrue(limiter.allow("alice"))
    }
}
