package com.subhrodip.pennywise.expensecore.groups.persistence.store
import com.subhrodip.pennywise.expensecore.groups.api.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.api.CreateInviteRequest
import com.subhrodip.pennywise.expensecore.groups.api.CreatePlaceholderRequest
import com.subhrodip.pennywise.expensecore.groups.api.GroupMemberResponse
import com.subhrodip.pennywise.expensecore.groups.api.GroupResponse
import com.subhrodip.pennywise.expensecore.groups.api.InviteResponse
import com.subhrodip.pennywise.expensecore.groups.api.UpdateGroupRequest
import com.subhrodip.pennywise.ids.generation.UuidGenerator
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory implementation of [GroupStore] used for testing.
 */
class InMemoryGroupStore : GroupStore {
    private val groupsByMember = ConcurrentHashMap<String, MutableSet<UUID>>()
    private val groups = ConcurrentHashMap<UUID, GroupResponse>()
    private val invites = ConcurrentHashMap<String, InviteData>()
    private val memberships = ConcurrentHashMap<UUID, MutableList<GroupMemberResponse>>()

    private data class InviteData(
        val groupId: UUID,
        val expiresAt: Instant,
        val placeholderId: UUID? = null,
        var claimedAt: Instant? = null,
        var claimedBy: String? = null,
        var revokedAt: Instant? = null
    )

    override fun create(subject: String, request: CreateGroupRequest): GroupResponse {
        val group = GroupResponse(UuidGenerator.next(), request.name.trim(), request.kind, 0, "ACTIVE")
        groups[group.groupId] = group
        groupsByMember.computeIfAbsent(subject) { ConcurrentHashMap.newKeySet() }.add(group.groupId)
        memberships.computeIfAbsent(group.groupId) { CopyOnWriteArrayList() }
            .add(GroupMemberResponse(UuidGenerator.next(), group.groupId, subject, null, false, "ACTIVE"))
        return group
    }

    override fun list(subject: String): List<GroupResponse> =
        groupsByMember[subject].orEmpty().mapNotNull(groups::get).filter { it.status == "ACTIVE" }

    override fun update(groupId: UUID, subject: String, request: UpdateGroupRequest): GroupResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val existing = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (existing.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_06, "Group is archived")
        }
        val updated = existing.copy(name = request.name.trim(), revision = existing.revision + 1)
        groups[groupId] = updated
        return updated
    }

    override fun archive(groupId: UUID, subject: String): GroupResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val existing = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (existing.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_06, "Group is already archived")
        }
        val updated = existing.copy(status = "ARCHIVED", revision = existing.revision + 1)
        groups[groupId] = updated
        return updated
    }

    override fun addPlaceholder(groupId: UUID, subject: String, request: CreatePlaceholderRequest): GroupMemberResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val group = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (group.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_04, "Group is archived")
        }
        val member = GroupMemberResponse(
            membershipId = UuidGenerator.next(),
            groupId = groupId,
            subject = null,
            displayName = request.name.trim(),
            isPlaceholder = true,
            status = "ACTIVE"
        )
        memberships.computeIfAbsent(groupId) { CopyOnWriteArrayList() }.add(member)
        groups[groupId] = group.copy(revision = group.revision + 1)
        return member
    }

    override fun removeMember(groupId: UUID, subject: String, membershipId: UUID) {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val group = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (group.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_04, "Group is archived")
        }
        val list = memberships[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Member not found")
        val index = list.indexOfFirst { it.membershipId == membershipId }
        if (index == -1) {
            throw ApplicationException(ErrorCode.ERR_05, "Member not found")
        }
        val existing = list[index]
        if (existing.status == "REMOVED") {
            throw ApplicationException(ErrorCode.ERR_06, "Member is already removed")
        }
        list[index] = existing.copy(status = "REMOVED")
        if (existing.subject != null) {
            groupsByMember[existing.subject]?.remove(groupId)
        }
        groups[groupId] = group.copy(revision = group.revision + 1)
    }

    override fun listMembers(groupId: UUID, subject: String): List<GroupMemberResponse> {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        if (groups[groupId]?.status != "ACTIVE") {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        return memberships[groupId].orEmpty().filter { it.status == "ACTIVE" }
    }

    override fun invite(groupId: UUID, subject: String, request: CreateInviteRequest): InviteResponse {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val group = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (group.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_04, "Group is archived")
        }
        if (request.placeholderId != null) {
            val list = memberships[groupId].orEmpty()
            val target = list.find { it.membershipId == request.placeholderId }
            if (target == null || !target.isPlaceholder || target.status != "ACTIVE" || target.subject != null) {
                throw ApplicationException(ErrorCode.ERR_05, "Placeholder not found or already bound")
            }
        }
        val token = UuidGenerator.next().toString().replace("-", "") + UuidGenerator.next().toString().replace("-", "")
        val expiry = Instant.now().plus(Duration.ofHours(request.expiresInHours.toLong()))
        invites[token] = InviteData(groupId, expiry, request.placeholderId)
        return InviteResponse(token, expiry, request.placeholderId)
    }

    override fun revokeInvite(groupId: UUID, subject: String, token: String) {
        if (groupsByMember[subject]?.contains(groupId) != true) {
            throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        }
        val group = groups[groupId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
        if (group.status == "ARCHIVED") {
            throw ApplicationException(ErrorCode.ERR_04, "Group is archived")
        }
        val data = invites[token]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Invite not found")
        if (data.groupId != groupId || data.claimedAt != null || data.revokedAt != null) {
            throw ApplicationException(ErrorCode.ERR_06, "Invite cannot be revoked")
        }
        data.revokedAt = Instant.now()
        groups[groupId] = group.copy(revision = group.revision + 1)
    }

    override fun claim(token: String, subject: String): GroupResponse {
        if (!token.matches(Regex("^[a-f0-9]{64}$"))) {
            throw ApplicationException(ErrorCode.ERR_06, "Invite is invalid or already claimed")
        }
        val data = invites[token]
            ?: throw ApplicationException(ErrorCode.ERR_06, "Invite is invalid or already claimed")
        synchronized(data) {
            if (data.claimedAt != null || data.revokedAt != null || !data.expiresAt.isAfter(Instant.now())) {
                throw ApplicationException(ErrorCode.ERR_06, "Invite is invalid, expired, or already claimed/revoked")
            }
            val group = groups[data.groupId]
                ?: throw ApplicationException(ErrorCode.ERR_05, "Group not found")
            if (group.status == "ARCHIVED") {
                throw ApplicationException(ErrorCode.ERR_04, "Group is archived")
            }

            val memberList = memberships.computeIfAbsent(group.groupId) { CopyOnWriteArrayList() }
            if (data.placeholderId != null) {
                val index = memberList.indexOfFirst { it.membershipId == data.placeholderId }
                if (index == -1) {
                    throw ApplicationException(ErrorCode.ERR_05, "Placeholder not found")
                }
                val target = memberList[index]
                if (target.status != "ACTIVE" || target.subject != null) {
                    throw ApplicationException(ErrorCode.ERR_06, "Placeholder is no longer available")
                }
                data.claimedAt = Instant.now()
                data.claimedBy = subject
                memberList[index] = target.copy(subject = subject, isPlaceholder = false)
            } else {
                if (memberList.any { it.subject == subject && it.status == "ACTIVE" }) {
                    return group
                }
                data.claimedAt = Instant.now()
                data.claimedBy = subject
                memberList.add(GroupMemberResponse(UuidGenerator.next(), group.groupId, subject, null, false, "ACTIVE"))
            }
            groupsByMember.computeIfAbsent(subject) { ConcurrentHashMap.newKeySet() }.add(group.groupId)
            val updatedGroup = group.copy(revision = group.revision + 1)
            groups[group.groupId] = updatedGroup
            return updatedGroup
        }
    }
}
