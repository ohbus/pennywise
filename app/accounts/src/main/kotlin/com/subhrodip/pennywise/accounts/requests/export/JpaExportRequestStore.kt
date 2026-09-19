package com.subhrodip.pennywise.accounts.requests.export

import com.subhrodip.pennywise.accounts.ProfileRules
import com.subhrodip.pennywise.ids.UuidGenerator
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * JPA-backed implementation of [ExportRequestStore].
 *
 * Persists data export requests to PostgreSQL via [ExportRequestRepository].
 *
 * @param repository Spring Data JPA repository for export requests.
 * @param clock Timestamp supplier, defaulting to [Instant.now].
 */
@Primary
@Service
class JpaExportRequestStore(
    private val repository: ExportRequestRepository,
    private val clock: () -> Instant = Instant::now
) : ExportRequestStore {

    /**
     * Creates and persists a new export request with a newly generated UUID.
     *
     * @param subject OIDC subject identifier.
     * @return Saved [ExportRequest].
     */
    @Transactional
    override fun request(subject: String): ExportRequest {
        ProfileRules.requireSubject(subject)
        val entity = AccountExportRequestEntity(
            exportId = UuidGenerator.next(),
            subject = subject,
            status = ExportStatus.REQUESTED,
            requestedAt = clock()
        )
        return repository.save(entity).toRecord()
    }

    /**
     * Retrieves an export request by ID.
     *
     * @param exportId Unique identifier of the export request.
     * @return [ExportRequest] or null if not found.
     */
    @Transactional(readOnly = true)
    override fun get(exportId: UUID): ExportRequest? {
        return repository.findById(exportId).map { it.toRecord() }.orElse(null)
    }

    /**
     * Retrieves all export requests for a subject ordered by requested timestamp descending.
     *
     * @param subject OIDC subject identifier.
     * @return List of matching [ExportRequest]s.
     */
    @Transactional(readOnly = true)
    override fun listBySubject(subject: String): List<ExportRequest> {
        ProfileRules.requireSubject(subject)
        return repository.findBySubjectOrderByRequestedAtDesc(subject).map { it.toRecord() }
    }
}

/**
 * Maps [AccountExportRequestEntity] to [ExportRequest] domain model.
 */
private fun AccountExportRequestEntity.toRecord() = ExportRequest(
    exportId = exportId,
    subject = subject,
    requestedAt = requestedAt,
    status = status
)
