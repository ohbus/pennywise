package com.subhrodip.pennywise.notifications

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Isolated transactional processor responsible for persisting inbox items and recording processed events.
 */
@Service
class TransactionalNotificationEventProcessor(
    private val processedEvents: ProcessedNotificationEventRepository,
    private val entityManager: EntityManager
) {
    /**
     * Atomically checks for duplicate events and persists the notification inbox record and event receipt.
     *
     * @param event The notification event to process.
     * @return [NotificationConsumptionOutcome.APPLIED] if newly persisted, or [NotificationConsumptionOutcome.DUPLICATE] if previously recorded.
     */
    @Transactional
    fun process(event: NotificationEvent): NotificationConsumptionOutcome {
        val inboxItem = event.toInboxEntity()
        if (processedEvents.existsById(event.eventId)) {
            return NotificationConsumptionOutcome.DUPLICATE
        }

        entityManager.persist(ProcessedNotificationEventEntity(event.eventId, Instant.now()))
        entityManager.persist(inboxItem)
        entityManager.flush()
        return NotificationConsumptionOutcome.APPLIED
    }
}

private fun NotificationEvent.toInboxEntity(): NotificationInboxEntity {
    require(subject.isNotBlank() && subject.length <= 200) {
        "notification subject must contain between 1 and 200 characters"
    }
    require(eventType.isNotBlank() && eventType.length <= 200) {
        "event type must contain between 1 and 200 characters"
    }
    require(message.isNotBlank() && message.length <= 2000) {
        "message must contain between 1 and 2000 characters"
    }
    return NotificationInboxEntity(notificationId, subject, eventType, message, occurredAt)
}
