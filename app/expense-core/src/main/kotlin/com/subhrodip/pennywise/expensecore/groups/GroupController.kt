package com.subhrodip.pennywise.expensecore.groups

import com.subhrodip.pennywise.ids.ApiEndpoints
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.security.Principal
import java.time.Instant
import java.util.UUID

data class CreateGroupRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:NotBlank @field:Pattern(regexp = "^(HOUSEHOLD|COUPLE|TRIP)$") val kind: String,
    @field:Pattern(regexp = "^[A-Z]{3}$") val currency: String
)

data class UpdateGroupRequest(
    @field:NotBlank @field:Size(max = 120) val name: String
)

data class CreatePlaceholderRequest(
    @field:NotBlank @field:Size(max = 120) val name: String
)

data class GroupResponse(
    val groupId: UUID,
    val name: String,
    val kind: String,
    val revision: Long,
    val status: String = "ACTIVE"
)

data class GroupMemberResponse(
    @get:com.fasterxml.jackson.annotation.JsonProperty("membershipId") val membershipId: UUID,
    @get:com.fasterxml.jackson.annotation.JsonProperty("groupId") val groupId: UUID,
    @get:com.fasterxml.jackson.annotation.JsonProperty("subject") val subject: String? = null,
    @get:com.fasterxml.jackson.annotation.JsonProperty("displayName") val displayName: String? = null,
    @get:com.fasterxml.jackson.annotation.JsonProperty("isPlaceholder") val isPlaceholder: Boolean = false,
    @get:com.fasterxml.jackson.annotation.JsonProperty("status") val status: String = "ACTIVE"
)

data class CreateInviteRequest(
    @field:Min(1)
    @field:Max(168)
    val expiresInHours: Int,
    @get:com.fasterxml.jackson.annotation.JsonProperty("placeholderId") val placeholderId: UUID? = null
)

data class InviteResponse(
    val token: String,
    val expiresAt: Instant,
    @get:com.fasterxml.jackson.annotation.JsonProperty("placeholderId") val placeholderId: UUID? = null
)

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS)
class GroupController(private val groups: GroupStore) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateGroupRequest, principal: Principal): GroupResponse =
        groups.create(principal.name, request)

    @GetMapping
    fun list(principal: Principal): List<GroupResponse> = groups.list(principal.name)

    @GetMapping("/{groupId}")
    fun get(@PathVariable groupId: UUID, principal: Principal): GroupResponse =
        groups.list(principal.name).find { it.groupId == groupId }
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group $groupId not found")

    @PatchMapping("/{groupId}")
    fun update(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpdateGroupRequest,
        @RequestHeader("X-Acceptance-Fault", required = false) fault: String?,
        principal: Principal
    ): GroupResponse {
        if (fault == "rollback") throw ApplicationException(ErrorCode.ERR_06, "Acceptance rollback fault")
        return groups.update(groupId, principal.name, request)
    }

    @PostMapping("/{groupId}/archive")
    fun archive(@PathVariable groupId: UUID, principal: Principal): GroupResponse =
        groups.archive(groupId, principal.name)

    @PostMapping("/{groupId}/placeholders")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPlaceholder(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: CreatePlaceholderRequest,
        principal: Principal
    ): GroupMemberResponse = groups.addPlaceholder(groupId, principal.name, request)

    @DeleteMapping("/{groupId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        @PathVariable groupId: UUID,
        @PathVariable membershipId: UUID,
        principal: Principal
    ) {
        groups.removeMember(groupId, principal.name, membershipId)
    }

    @GetMapping("/{groupId}/members")
    fun listMembers(
        @PathVariable groupId: UUID,
        @RequestHeader("X-Acceptance-Fault", required = false) fault: String?,
        principal: Principal
    ): List<GroupMemberResponse> {
        if (fault == "fanout") throw ApplicationException(ErrorCode.ERR_08, "Acceptance fanout fault")
        return groups.listMembers(groupId, principal.name)
    }

    @PostMapping("/{groupId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(@PathVariable groupId: UUID, @Valid @RequestBody request: CreateInviteRequest, principal: Principal): InviteResponse =
        groups.invite(groupId, principal.name, request)

    @PostMapping("/{groupId}/invites/{token}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeInvite(
        @PathVariable groupId: UUID,
        @PathVariable token: String,
        principal: Principal
    ) {
        groups.revokeInvite(groupId, principal.name, token)
    }
}

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.BASE + "/invites")
class InviteClaimController(private val groups: GroupStore) {
    @PostMapping("/{token}/claim")
    fun claim(@PathVariable token: String, principal: Principal): GroupResponse =
        groups.claim(token, principal.name)
}
