package com.subhrodip.pennywise.notifications.inbox.model

import java.time.Instant
import java.util.UUID

/** One immutable notification entry exposed in the user's inbox. */
data class InboxItem(
    val notificationId: UUID,
    val eventType: String,
    val message: String,
    val occurredAt: Instant,
    val read: Boolean = false
)
