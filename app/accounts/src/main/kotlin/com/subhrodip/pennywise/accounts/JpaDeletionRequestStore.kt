package com.subhrodip.pennywise.accounts

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * JPA-backed implementation of [DeletionRequestStore].
 *
 * Persists deletion requests to PostgreSQL via [DeletionRequestRepository] while updating
 * the user profile record via [ProfileStore] to preserve historical financial attribution.
 *
 * @param repository Spring Data JPA repository for deletion request entities.
 * @param profileStore Store used to flag profiles as deletion-requested.
 * @param clock Timestamp supplier, defaulting to [Instant.now].
 */
@Primary
@Service
class JpaDeletionRequestStore(
    private val repository: DeletionRequestRepository,
    private val profileStore: ProfileStore,
    private val clock: () -> Instant = Instant::now
) : DeletionRequestStore {

    /**
     * Records a new deletion request for the subject or returns an existing one.
     * Preserves profile record with `deletionRequested = true` for financial attribution.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] representing the saved or existing request.
     */
    @Transactional
    override fun request(subject: String): DeletionRequest {
        ProfileRules.requireSubject(subject)
        val existing = repository.findById(subject).orElse(null)
        if (existing != null) {
            return existing.toRecord()
        }

        // Retain historical financial attribution by preserving profile record with deletionRequested = true
        profileStore.requestDeletion(subject)

        val entity = AccountDeletionRequestEntity(
            subject = subject,
            status = DeletionStatus.REQUESTED,
            requestedAt = clock()
        )
        return repository.save(entity).toRecord()
    }

    /**
     * Retrieves the deletion request entity for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] or null if not found.
     */
    @Transactional(readOnly = true)
    override fun get(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        return repository.findById(subject).map { it.toRecord() }.orElse(null)
    }

    /**
     * Cancels an existing deletion request for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return Updated [DeletionRequest] with CANCELLED status, or null if none existed.
     */
    @Transactional
    override fun cancel(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val entity = repository.findById(subject).orElse(null) ?: return null
        entity.status = DeletionStatus.CANCELLED
        return repository.save(entity).toRecord()
    }

    /**
     * Marks an existing deletion request as completed.
     *
     * @param subject OIDC subject identifier.
     * @return Updated [DeletionRequest] with COMPLETED status, or null if none existed.
     */
    @Transactional
    override fun complete(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val entity = repository.findById(subject).orElse(null) ?: return null
        entity.status = DeletionStatus.COMPLETED
        return repository.save(entity).toRecord()
    }
}

/**
 * Maps [AccountDeletionRequestEntity] to [DeletionRequest] domain model.
 */
private fun AccountDeletionRequestEntity.toRecord() = DeletionRequest(
    subject = subject,
    requestedAt = requestedAt,
    status = status
)
