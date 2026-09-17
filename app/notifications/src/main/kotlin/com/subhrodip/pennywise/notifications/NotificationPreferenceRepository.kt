package com.subhrodip.pennywise.notifications

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [NotificationPreferenceEntity] persistence operations.
 */
@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreferenceEntity, String>
