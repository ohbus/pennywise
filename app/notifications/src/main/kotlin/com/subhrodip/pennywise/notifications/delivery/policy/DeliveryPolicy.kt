package com.subhrodip.pennywise.notifications.delivery.policy

import com.subhrodip.pennywise.notifications.delivery.model.DeliveryChannel
import com.subhrodip.pennywise.notifications.delivery.model.DeliveryDecision
import com.subhrodip.pennywise.notifications.delivery.persistence.EventDeduplicator
/**
 * Defines the delivery policy for notification events.
 */

import com.subhrodip.pennywise.notifications.preferences.persistence.PreferenceStore
import java.util.UUID

class DeliveryPolicy(private val inbox: EventDeduplicator, private val preferences: PreferenceStore) {
    fun decide(eventId: UUID, subject: String): DeliveryDecision? {
        if (!inbox.firstDelivery(eventId)) return null
        val pref = preferences.get(subject)
        return DeliveryDecision(eventId, subject, buildSet {
            if (pref.emailEnabled) add(DeliveryChannel.EMAIL)
            if (pref.pushEnabled) add(DeliveryChannel.PUSH)
        })
    }
}
