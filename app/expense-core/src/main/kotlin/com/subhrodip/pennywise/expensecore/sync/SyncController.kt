package com.subhrodip.pennywise.expensecore.sync

import java.security.Principal
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipRepository

import com.subhrodip.pennywise.ids.ApiEndpoints

data class SyncChangeResponse(val revision: Long, val entityId: String, val deleted: Boolean, val payload: String?)
data class SyncPageResponse(val changes: List<SyncChangeResponse>, val nextCursor: String?, val hasMore: Boolean)

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.PATH_GROUP_SYNC)
class SyncController(
    private val store: SynchronizationStore,
    private val memberships: GroupMembershipRepository,
) {
    @GetMapping(ApiEndpoints.ExpenseCore.V1.SYNC_SNAPSHOT_RELATIVE_SUBPATH, ApiEndpoints.ExpenseCore.V1.SYNC_CHANGES_RELATIVE_SUBPATH)
    fun page(
        @PathVariable groupId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int,
        principal: Principal
    ): SyncPageResponse {
        if (principal.name.isBlank()) throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is required")
        if (!memberships.existsByGroupIdAndSubjectAndStatus(groupId, principal.name, "ACTIVE")) {
            throw ApplicationException(ErrorCode.ERR_05, "Group $groupId not found")
        }
        if (limit !in 1..100) throw ApplicationException(ErrorCode.ERR_02, "limit must be between 1 and 100")
        return try {
            store.snapshot(groupId.toString(), cursor, limit).toResponse()
        } catch (exception: InvalidSyncCursorException) {
            throw ApplicationException(ErrorCode.ERR_02, exception.message ?: "Invalid sync cursor", exception)
        }
    }
}

private fun SyncPage.toResponse(): SyncPageResponse = SyncPageResponse(
    changes = changes.map { SyncChangeResponse(it.revision, it.entityId, it.deleted, it.payload) },
    nextCursor = nextCursor,
    hasMore = hasMore
)
