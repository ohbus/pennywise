package com.subhrodip.pennywise.accounts

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Port interface for managing and persisting account deletion requests.
 */
interface DeletionRequestStore {
    /**
     * Submits or retrieves an existing deletion request for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] representing the submitted or existing request.
     */
    fun request(subject: String): DeletionRequest

    /**
     * Retrieves the current deletion request for the given subject, if any.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] or null if not found.
     */
    fun get(subject: String): DeletionRequest?

    /**
     * Cancels an existing deletion request for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return Cancelled [DeletionRequest], or null if no request existed.
     */
    fun cancel(subject: String): DeletionRequest?

    /**
     * Marks an existing deletion request as completed for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return Completed [DeletionRequest], or null if no request existed.
     */
    fun complete(subject: String): DeletionRequest?
}

/**
 * In-memory thread-safe implementation of [DeletionRequestStore] primarily used in testing and local fallback.
 *
 * @param clock Function providing the current timestamp, defaults to [Instant.now].
 */
class InMemoryDeletionRequestStore(
    private val clock: () -> Instant = Instant::now
) : DeletionRequestStore {
    private val requests = ConcurrentHashMap<String, DeletionRequest>()

    /**
     * Submits or retrieves an existing deletion request in-memory.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] representing the submitted or existing request.
     */
    override fun request(subject: String): DeletionRequest {
        ProfileRules.requireSubject(subject)
        return requests.compute(subject) { _, existing ->
            existing ?: DeletionRequest(subject, clock())
        }!!
    }

    /**
     * Retrieves the in-memory deletion request for the subject.
     *
     * @param subject OIDC subject identifier.
     * @return [DeletionRequest] or null if absent.
     */
    override fun get(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        return requests[subject]
    }

    /**
     * Cancels the deletion request in-memory.
     *
     * @param subject OIDC subject identifier.
     * @return Updated [DeletionRequest] with CANCELLED status, or null if none existed.
     */
    override fun cancel(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val existing = requests[subject] ?: return null
        val updated = existing.copy(status = DeletionStatus.CANCELLED)
        requests[subject] = updated
        return updated
    }

    /**
     * Marks the deletion request as completed in-memory.
     *
     * @param subject OIDC subject identifier.
     * @return Updated [DeletionRequest] with COMPLETED status, or null if none existed.
     */
    override fun complete(subject: String): DeletionRequest? {
        ProfileRules.requireSubject(subject)
        val existing = requests[subject] ?: return null
        val updated = existing.copy(status = DeletionStatus.COMPLETED)
        requests[subject] = updated
        return updated
    }
}
