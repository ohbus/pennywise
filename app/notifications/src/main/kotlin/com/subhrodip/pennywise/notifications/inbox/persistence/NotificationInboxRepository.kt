package com.subhrodip.pennywise.notifications.inbox.persistence

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for [NotificationInboxEntity] operations.
 */
@Repository
interface NotificationInboxRepository : JpaRepository<NotificationInboxEntity, UUID> {
    /**
     * Retrieves all notifications for an authenticated subject using the specified sort order.
     *
     * @param subject Authenticated recipient subject.
     * @param sort Sort specification (typically occurredAt DESC, notificationId DESC).
     * @return List of matching inbox entities.
     */
    fun findAllBySubject(subject: String, sort: Sort): List<NotificationInboxEntity>

    /**
     * Finds a single notification inbox entry by recipient subject and notification ID.
     *
     * @param subject Authenticated recipient subject.
     * @param notificationId Unique notification identifier.
     * @return Matching inbox entity or null if not found.
     */
    fun findBySubjectAndNotificationId(subject: String, notificationId: UUID): NotificationInboxEntity?
}
