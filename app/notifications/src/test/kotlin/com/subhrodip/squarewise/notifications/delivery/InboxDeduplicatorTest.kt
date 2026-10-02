package com.subhrodip.squarewise.notifications.delivery

import com.subhrodip.squarewise.notifications.delivery.model.DeliveryChannel
import com.subhrodip.squarewise.notifications.delivery.model.DeliveryOutcome
import com.subhrodip.squarewise.notifications.delivery.model.RetryDecision
import com.subhrodip.squarewise.notifications.delivery.persistence.EventDeduplicator
import com.subhrodip.squarewise.notifications.delivery.persistence.InboxDeduplicator
import com.subhrodip.squarewise.notifications.delivery.policy.DeliveryPolicy
import com.subhrodip.squarewise.notifications.delivery.policy.RetryPolicy
import com.subhrodip.squarewise.notifications.delivery.rate.DeliveryRateLimiter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class InboxDeduplicatorTest {
    @Test
    fun `accepts first delivery and rejects duplicate`() {
        val inbox = InboxDeduplicator()
        val eventId = UUID.randomUUID()
        assertTrue(inbox.firstDelivery(eventId))
        assertFalse(inbox.firstDelivery(eventId))
        assertEquals(1, inbox.size())
    }
}
