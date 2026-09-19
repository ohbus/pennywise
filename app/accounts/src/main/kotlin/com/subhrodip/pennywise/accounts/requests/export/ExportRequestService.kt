package com.subhrodip.pennywise.accounts.requests.export

import java.time.Instant
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

enum class ExportStatus { REQUESTED, READY, EXPIRED }
data class ExportRequest(val exportId: UUID, val subject: String, val requestedAt: Instant, var status: ExportStatus = ExportStatus.REQUESTED)

@Service
class ExportRequestService(
    private val store: ExportRequestStore = InMemoryExportRequestStore()
) {
    constructor(clock: () -> Instant) : this(InMemoryExportRequestStore(clock))

    fun request(subject: String): ExportRequest = store.request(subject)
    fun get(exportId: UUID): ExportRequest? = store.get(exportId)
    fun listBySubject(subject: String): List<ExportRequest> = store.listBySubject(subject)
}
