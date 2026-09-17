package com.subhrodip.pennywise.expensecore.groups

import com.subhrodip.pennywise.ids.UuidGenerator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory implementation of [GroupStore] used for unit testing.
 */
@Service
class InMemoryGroupStore : GroupStore {
    private val groupsByMember = ConcurrentHashMap<String, MutableSet<UUID>>()
    private val groups = ConcurrentHashMap<UUID, GroupResponse>()
    private val invites = ConcurrentHashMap<String, Pair<UUID, Instant>>()
    private val memberships = ConcurrentHashMap<UUID, MutableList<GroupMemberResponse>>()

    override fun create(subject: String, request: CreateGroupRequest): GroupResponse {
        val group = GroupResponse(UuidGenerator.next(), request.name.trim(), 0)
        groups[group.groupId] = group
        groupsByMember.computeIfAbsent(subject) { ConcurrentHashMap.newKeySet() }.add(group.groupId)
        memberships.computeIfAbsent(group.groupId) { CopyOnWriteArrayList() }
            .add(GroupMemberResponse(UuidGenerator.next(), group.groupId, subject))
        return group
    }

    override fun list(subject: String): List<GroupResponse> = groupsByMember[subject].orEmpty().mapNotNull(groups::get)

    override fun update(groupId: UUID, subject: String, request: UpdateGroupRequest): GroupResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        }
        val existing = groups[groupId]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        val updated = existing.copy(name = request.name.trim(), revision = existing.revision + 1)
        groups[groupId] = updated
        return updated
    }

    override fun listMembers(groupId: UUID, subject: String): List<GroupMemberResponse> {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        }
        return memberships[groupId].orEmpty().toList()
    }

    override fun invite(groupId: UUID, subject: String, request: CreateInviteRequest): InviteResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        }
        val token = UuidGenerator.next().toString().replace("-", "") + UuidGenerator.next().toString().replace("-", "")
        val expiry = Instant.now().plus(Duration.ofHours(request.expiresInHours.toLong()))
        invites[token] = groupId to expiry
        return InviteResponse(token, expiry)
    }

    override fun claim(token: String, subject: String): GroupResponse {
        if (!token.matches(Regex("^[a-f0-9]{64}$"))) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Invite is invalid or already claimed")
        }
        val invite = invites.remove(token)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Invite is invalid or already claimed")
        if (!invite.second.isAfter(Instant.now())) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Invite has expired")
        }
        val group = groups[invite.first]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")
        groupsByMember.computeIfAbsent(subject) { ConcurrentHashMap.newKeySet() }.add(group.groupId)
        val memberList = memberships.computeIfAbsent(group.groupId) { CopyOnWriteArrayList() }
        if (memberList.none { it.subject == subject }) {
            memberList.add(GroupMemberResponse(UuidGenerator.next(), group.groupId, subject))
        }
        return group
    }
}
