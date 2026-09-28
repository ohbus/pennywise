package com.subhrodip.pennywise.notifications.consumer.model

import java.time.Instant
import java.util.UUID

/** Broker event consumed into the notification inbox. */
data class NotificationEvent(
    val eventId: UUID,
    val notificationId: UUID,
    val subject: String,
    val eventType: String,
    val message: String,
    val occurredAt: Instant,
    val recipientEmail: String? = null,
    val title: String = eventType,
    val body: String = message
) { val recipientId: String get() = subject }
