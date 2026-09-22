package com.subhrodip.pennywise.notifications.delivery.model

import java.util.UUID

/** Selected channels for one accepted notification delivery. */
data class DeliveryDecision(val eventId: UUID, val subject: String, val channels: Set<DeliveryChannel>)
