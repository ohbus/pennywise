package com.subhrodip.pennywise.notifications.delivery

import com.subhrodip.pennywise.notifications.delivery.model.DeliveryChannel
import com.subhrodip.pennywise.notifications.delivery.model.DeliveryOutcome
import com.subhrodip.pennywise.notifications.delivery.model.RetryDecision
import com.subhrodip.pennywise.notifications.delivery.persistence.EventDeduplicator
import com.subhrodip.pennywise.notifications.delivery.persistence.InboxDeduplicator
import com.subhrodip.pennywise.notifications.delivery.policy.DeliveryPolicy
import com.subhrodip.pennywise.notifications.delivery.policy.RetryPolicy
import com.subhrodip.pennywise.notifications.delivery.rate.DeliveryRateLimiter

import com.subhrodip.pennywise.notifications.preferences.persistence.InMemoryPreferenceStore
import com.subhrodip.pennywise.notifications.preferences.model.NotificationPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DeliveryPolicyTest {
    @Test
    fun `honors preferences and suppresses duplicate event`() {
        val store = InMemoryPreferenceStore()
        store.put("alice", NotificationPreferences(emailEnabled = false, pushEnabled = true))
        val policy = DeliveryPolicy(InboxDeduplicator(), store)
        val event = UUID.randomUUID()
        assertEquals(setOf(DeliveryChannel.PUSH), policy.decide(event, "alice")!!.channels)
        assertNull(policy.decide(event, "alice"))
    }
}
