package com.subhrodip.pennywise.notifications.delivery

import com.subhrodip.pennywise.notifications.consumer.ProcessedNotificationEventEntity
import jakarta.persistence.EntityManager
import org.hibernate.exception.ConstraintViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

/**
 * JPA and database-backed implementation of [EventDeduplicator] utilizing unique constraint violations
 * on [ProcessedNotificationEventEntity] table to guarantee at-most-once processing across concurrent deliveries.
 *
 * @property transactionManager Spring transaction manager used to manage isolation of deduplication checks.
 * @property entityManager Entity manager used for persisting processed event markers.
 */
@Service
class JpaEventDeduplicator(
    transactionManager: PlatformTransactionManager,
    private val entityManager: EntityManager
) : EventDeduplicator {
    private val transaction = TransactionTemplate(transactionManager)

    /**
     * Determines whether an event with the specified [eventId] is being processed for the first time.
     *
     * @param eventId Unique identifier of the incoming event.
     * @return `true` if successfully recorded as first delivery; `false` if a constraint violation indicates a duplicate.
     */
    override fun firstDelivery(eventId: UUID): Boolean = try {
        transaction.execute {
            entityManager.persist(ProcessedNotificationEventEntity(eventId, Instant.now()))
            entityManager.flush()
            true
        }
    } catch (_: ConstraintViolationException) {
        false
    }
}
