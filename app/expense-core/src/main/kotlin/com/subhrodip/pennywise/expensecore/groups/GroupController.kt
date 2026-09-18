package com.subhrodip.pennywise.expensecore.groups

import com.subhrodip.pennywise.ids.UuidGenerator
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.util.UUID
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

import com.subhrodip.pennywise.ids.ApiEndpoints

data class CreateGroupRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:NotBlank @field:Pattern(regexp = "^(HOUSEHOLD|COUPLE|TRIP)$") val kind: String,
    @field:Pattern(regexp = "^[A-Z]{3}$") val currency: String
)

data class UpdateGroupRequest(
    @field:NotBlank @field:Size(max = 120) val name: String
)

data class GroupResponse(val groupId: UUID, val name: String, val revision: Long)

data class GroupMemberResponse(val membershipId: UUID, val groupId: UUID, val subject: String)

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
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group $groupId not found")

    @PatchMapping("/{groupId}")
    fun update(@PathVariable groupId: UUID, @Valid @RequestBody request: UpdateGroupRequest, principal: Principal): GroupResponse =
        groups.update(groupId, principal.name, request)

    @GetMapping("/{groupId}/members")
    fun listMembers(@PathVariable groupId: UUID, principal: Principal): List<GroupMemberResponse> =
        groups.listMembers(groupId, principal.name)

    @PostMapping("/{groupId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(@PathVariable groupId: UUID, @Valid @RequestBody request: CreateInviteRequest, principal: Principal): InviteResponse =
        groups.invite(groupId, principal.name, request)

}

@RestController
@RequestMapping(ApiEndpoints.ExpenseCore.V1.BASE + "/invites")
class InviteClaimController(private val groups: GroupStore) {
    @PostMapping("/{token}/claim")
    fun claim(@PathVariable token: String, @AuthenticationPrincipal principal: Principal): GroupResponse = groups.claim(token, principal.name)
}

data class CreateInviteRequest(
    @field:Min(1)
    @field:Max(168)
    val expiresInHours: Int
)
data class InviteResponse(val token: String, val expiresAt: Instant)
