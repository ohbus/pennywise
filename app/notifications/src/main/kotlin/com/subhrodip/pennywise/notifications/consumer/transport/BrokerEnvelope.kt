package com.subhrodip.pennywise.notifications.consumer.transport

import java.time.Instant
import java.util.UUID

/** Validated broker envelope before conversion to the notification domain event. */
data class BrokerEnvelope(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val groupId: UUID,
    val groupRevision: Long,
    val occurredAt: Instant,
    val payload: Map<String, Any?>
)
