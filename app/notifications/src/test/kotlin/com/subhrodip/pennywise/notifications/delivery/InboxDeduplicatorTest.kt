package com.subhrodip.pennywise.notifications.delivery

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
import java.util.UUID

class InboxDeduplicatorTest {
    @Test
    fun `accepts first delivery and rejects duplicate`() {
        val inbox = InboxDeduplicator()
        val eventId = UUID.randomUUID()
        assertTrue(inbox.firstDelivery(eventId))
        assertFalse(inbox.firstDelivery(eventId))
    }
}
