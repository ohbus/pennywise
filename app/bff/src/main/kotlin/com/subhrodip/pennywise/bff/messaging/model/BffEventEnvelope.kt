package com.subhrodip.pennywise.bff.messaging.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant
import java.util.UUID

/**
 * Typed event envelope matching `contracts/events/envelope.schema.json`.
 *
 * Represents committed domain events consumed over RabbitMQ from Expense Core.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BffEventEnvelope(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val groupId: UUID,
    val groupRevision: Long,
    val occurredAt: Instant,
    val payload: Map<String, Any?> = emptyMap()
)
