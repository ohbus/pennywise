package com.subhrodip.pennywise.notifications.consumer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for [ProcessedNotificationEventEntity] tracking and deduplication queries.
 */
@Repository
interface ProcessedNotificationEventRepository : JpaRepository<ProcessedNotificationEventEntity, UUID>
