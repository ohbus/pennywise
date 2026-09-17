package com.subhrodip.pennywise.expensecore.sync

import java.security.Principal
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class SyncChangeResponse(val revision: Long, val entityId: String, val deleted: Boolean, val payload: String?)
data class SyncPageResponse(val changes: List<SyncChangeResponse>, val nextCursor: String?, val hasMore: Boolean)

@RestController
@RequestMapping("/expense-core/v1/groups/{groupId}/sync")
class SyncController(private val store: SynchronizationStore) {
    @GetMapping("/snapshot", "/changes")
    fun page(
        @PathVariable groupId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        principal: Principal
    ): SyncPageResponse {
        if (principal.name.isBlank()) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated subject is required")
        if (limit !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100")
        return try {
            store.snapshot(groupId.toString(), cursor, limit).toResponse()
        } catch (exception: InvalidSyncCursorException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, exception.message, exception)
        }
    }
}

private fun SyncPage.toResponse(): SyncPageResponse = SyncPageResponse(
    changes = changes.map { SyncChangeResponse(it.revision, it.entityId, it.deleted, it.payload) },
    nextCursor = nextCursor,
    hasMore = hasMore
)
