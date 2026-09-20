package com.subhrodip.pennywise.notifications.consumer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity tracking successfully processed notification event identifiers to guarantee
 * idempotent event handling and deduplication across broker deliveries.
 *
 * Maps to table `notification_processed_events`.
 *
 * @property eventId Unique identifier of the processed event.
 * @property processedAt Timestamp when the event record was persisted.
 */
@Entity
@Table(name = "notification_processed_events")
class ProcessedNotificationEventEntity(
    @Id
    @Column(name = "event_id", nullable = false)
    var eventId: UUID,
    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant
)
