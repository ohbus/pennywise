package com.subhrodip.pennywise.accounts.requests.export.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for [AccountExportRequestEntity].
 */
@Repository
interface ExportRequestRepository : JpaRepository<AccountExportRequestEntity, UUID> {
    /**
     * Retrieves all export requests for an OIDC subject ordered by requested timestamp descending.
     *
     * @param subject OIDC subject identifier.
     * @return List of matching [AccountExportRequestEntity] instances in reverse chronological order.
     */
    fun findBySubjectOrderByRequestedAtDesc(subject: String): List<AccountExportRequestEntity>
}
