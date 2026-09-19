package com.subhrodip.pennywise.accounts.requests.export

import com.subhrodip.pennywise.accounts.ProfileRules
import com.subhrodip.pennywise.ids.UuidGenerator
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Port interface for managing and persisting account data export requests.
 */
interface ExportRequestStore {
    /**
     * Creates and stores a new export request for the given subject.
     *
     * @param subject OIDC subject identifier.
     * @return [ExportRequest] representing the newly created request.
     */
    fun request(subject: String): ExportRequest

    /**
     * Retrieves an export request by its unique identifier.
     *
     * @param exportId Unique identifier of the export request.
     * @return [ExportRequest] or null if not found.
     */
    fun get(exportId: UUID): ExportRequest?

    /**
     * Lists all export requests for a subject, sorted in reverse chronological order.
     *
     * @param subject OIDC subject identifier.
     * @return List of [ExportRequest]s.
     */
    fun listBySubject(subject: String): List<ExportRequest>
}

/**
 * In-memory thread-safe implementation of [ExportRequestStore] primarily used in testing and local fallback.
 *
 * @param clock Function providing current timestamp, defaults to [Instant.now].
 */
class InMemoryExportRequestStore(
    private val clock: () -> Instant = Instant::now
) : ExportRequestStore {
    private val requests = ConcurrentHashMap<UUID, ExportRequest>()

    /**
     * Creates and stores a new export request in-memory.
     *
     * @param subject OIDC subject identifier.
     * @return Created [ExportRequest].
     */
    override fun request(subject: String): ExportRequest {
        ProfileRules.requireSubject(subject)
        val request = ExportRequest(UuidGenerator.next(), subject, clock())
        requests[request.exportId] = request
        return request
    }

    /**
     * Retrieves an export request by ID from memory.
     *
     * @param exportId Unique identifier of the export request.
     * @return [ExportRequest] or null if not found.
     */
    override fun get(exportId: UUID): ExportRequest? = requests[exportId]

    /**
     * Lists in-memory export requests for the specified subject sorted newest first.
     *
     * @param subject OIDC subject identifier.
     * @return Filtered and sorted list of [ExportRequest]s.
     */
    override fun listBySubject(subject: String): List<ExportRequest> {
        ProfileRules.requireSubject(subject)
        return requests.values
            .filter { it.subject == subject }
            .sortedByDescending { it.requestedAt }
    }
}
