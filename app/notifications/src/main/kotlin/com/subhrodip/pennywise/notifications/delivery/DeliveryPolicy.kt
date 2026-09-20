package com.subhrodip.pennywise.notifications.delivery

/**
 * Defines the delivery policy for notification events.
 */

import com.subhrodip.pennywise.notifications.preferences.PreferenceStore
import java.util.UUID

enum class DeliveryChannel { EMAIL, PUSH }
data class DeliveryDecision(val eventId: UUID, val subject: String, val channels: Set<DeliveryChannel>)

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
