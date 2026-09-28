package com.subhrodip.pennywise.notifications.delivery

import com.subhrodip.pennywise.notifications.delivery.model.DeliveryChannel
import com.subhrodip.pennywise.notifications.delivery.model.DeliveryOutcome
import com.subhrodip.pennywise.notifications.delivery.model.RetryDecision
import com.subhrodip.pennywise.notifications.delivery.persistence.EventDeduplicator
import com.subhrodip.pennywise.notifications.delivery.persistence.InboxDeduplicator
import com.subhrodip.pennywise.notifications.delivery.policy.DeliveryPolicy
import com.subhrodip.pennywise.notifications.delivery.policy.RetryPolicy
import com.subhrodip.pennywise.notifications.delivery.rate.DeliveryRateLimiter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class RetryPolicyTest {
    @Test
    fun `backs off retryable failures and parks after limit`() {
        val policy = RetryPolicy(3)
        assertEquals(Duration.ofSeconds(1), policy.decide(1, DeliveryOutcome.RETRYABLE_FAILURE).delay)
        assertEquals(Duration.ofSeconds(2), policy.decide(2, DeliveryOutcome.RETRYABLE_FAILURE).delay)
        assertEquals(true, policy.decide(3, DeliveryOutcome.RETRYABLE_FAILURE).parked)
        assertEquals(true, policy.decide(1, DeliveryOutcome.PERMANENT_FAILURE).parked)
    }
}
