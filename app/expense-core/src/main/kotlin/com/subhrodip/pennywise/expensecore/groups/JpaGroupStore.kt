package com.subhrodip.pennywise.expensecore.groups

import com.subhrodip.pennywise.expensecore.messaging.OutboxMessage
import com.subhrodip.pennywise.expensecore.messaging.OutboxStore
import com.subhrodip.pennywise.expensecore.sync.SynchronizationStore
import com.subhrodip.pennywise.ids.UuidGenerator
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * JPA persistence adapter implementing [GroupStore] for group lifecycle operations,
 * memberships, invitations, audit logging, synchronization records, and transactional outbox events.
 *
 * Invariants:
 * - Mutating operations run within transactional boundaries.
 * - Group renames atomically append an audit log entry, synchronization record, and outbox event.
 * - Invitations are strictly validated by format and expiration before claiming.
 */
@Primary
@Service
class JpaGroupStore(
    private val groups: GroupRepository,
    private val memberships: GroupMembershipRepository,
    private val invitations: GroupInvitationRepository,
    private val audit: GroupAuditRepository,
    private val synchronization: SynchronizationStore,
    private val outbox: OutboxStore
) : GroupStore {

    /**
     * Creates a new expense group with the calling subject as the initial member.
     *
     * @param subject the authenticated user subject creating the group
     * @param request the parameters for creating the group
     * @return the created group response
     */
    @Transactional
    override fun create(subject: String, request: CreateGroupRequest): GroupResponse {
        val entity = groups.save(
            GroupEntity(
                groupId = UuidGenerator.next(),
                name = request.name.trim(),
                kind = request.kind,
                currency = request.currency
            )
        )
        addMembership(entity.groupId, subject)
        return entity.toResponse()
    }

    /**
     * Lists all groups in which the given user subject is a registered member.
     *
     * @param subject the authenticated user subject
     * @return list of groups the subject belongs to
     */
    @Transactional(readOnly = true)
    override fun list(subject: String): List<GroupResponse> =
        memberships.findAllBySubjectOrderByMembershipId(subject)
            .mapNotNull { groups.findById(it.groupId).orElse(null)?.toResponse() }

    /**
     * Updates an existing expense group's name and increments its revision.
     *
     * Requires the modifying subject to be a member of the group. Emits audit and outbox events.
     *
     * @param groupId the UUID of the group to update
     * @param subject the authenticated user subject performing the update
     * @param request the updated group details
     * @return the updated group response with incremented revision
     * @throws ResponseStatusException if the group does not exist or the subject is not a member
     */
    @Transactional
    override fun update(groupId: UUID, subject: String, request: UpdateGroupRequest): GroupResponse {
        if (!memberships.existsByGroupIdAndSubject(groupId, subject)) notFound()
        val entity = groups.findForMembershipUpdate(groupId) ?: notFound()
        entity.name = request.name.trim()
        entity.revision += 1
        val saved = groups.save(entity)
        val occurredAt = Instant.now()
        val payload = mapOf(
            "groupId" to groupId.toString(),
            "name" to saved.name,
            "revision" to saved.revision,
            "changedBy" to subject
        )
        audit.save(
            GroupAuditEntity(
                auditId = UuidGenerator.next(),
                groupId = groupId,
                subject = subject,
                action = "group.renamed",
                revision = saved.revision,
                payload = payload.toString(),
                occurredAt = occurredAt
            )
        )
        synchronization.append(groupId.toString(), groupId.toString(), payload.toString())
        outbox.append(
            OutboxMessage(
                eventId = UuidGenerator.next(),
                eventType = "group.renamed.v1",
                aggregateId = groupId,
                groupId = groupId,
                groupRevision = saved.revision,
                occurredAt = occurredAt,
                payload = payload
            )
        )
        return saved.toResponse()
    }

    /**
     * Lists all members registered in the specified group.
     *
     * Requires the requesting subject to be a member of the group.
     *
     * @param groupId the UUID of the group
     * @param subject the requesting user subject
     * @return list of group members
     * @throws ResponseStatusException if the group does not exist or the subject is not a member
     */
    @Transactional(readOnly = true)
    override fun listMembers(groupId: UUID, subject: String): List<GroupMemberResponse> {
        if (!memberships.existsByGroupIdAndSubject(groupId, subject)) notFound()
        return memberships.findByGroupId(groupId).map {
            GroupMemberResponse(it.membershipId, it.groupId, it.subject)
        }
    }

    /**
     * Generates a new invitation token allowing other users to join the specified group.
     *
     * Requires the inviting subject to be an active member of the group.
     *
     * @param groupId the UUID of the group
     * @param subject the inviting user subject
     * @param request invitation configuration containing expiration hours
     * @return the generated token and expiration timestamp
     * @throws ResponseStatusException if the group does not exist or the subject is not a member
     */
    @Transactional
    override fun invite(groupId: UUID, subject: String, request: CreateInviteRequest): InviteResponse {
        if (!memberships.existsByGroupIdAndSubject(groupId, subject)) notFound()
        val token = invitationToken()
        val expiresAt = Instant.now().plus(Duration.ofHours(request.expiresInHours.toLong()))
        invitations.save(GroupInvitationEntity(token, groupId, expiresAt))
        return InviteResponse(token, expiresAt)
    }

    /**
     * Claims an invitation token, adding the caller subject to the group.
     *
     * @param token the 64-character hexadecimal invitation token
     * @param subject the claiming user subject
     * @return the joined group details
     * @throws ResponseStatusException if the token is invalid, expired, or already claimed
     */
    @Transactional
    override fun claim(token: String, subject: String): GroupResponse {
        if (!token.matches(INVITATION_TOKEN)) conflict()
        val claimedAt = Instant.now()
        if (invitations.claimIfAvailable(token, subject, claimedAt) != 1) conflict()
        val invitation = invitations.findById(token).orElseThrow(::conflict)
        val group = groups.findById(invitation.groupId).orElseThrow(::notFound)
        addMembership(group.groupId, subject)
        return group.toResponse()
    }

    private fun addMembership(groupId: UUID, subject: String) {
        groups.findForMembershipUpdate(groupId) ?: notFound()
        if (!memberships.existsByGroupIdAndSubject(groupId, subject)) {
            memberships.save(GroupMembershipEntity(UuidGenerator.next(), groupId, subject))
        }
    }

    private fun invitationToken(): String =
        UuidGenerator.next().toString().replace("-", "") + UuidGenerator.next().toString().replace("-", "")

    private fun conflict(): Nothing =
        throw ResponseStatusException(HttpStatus.CONFLICT, "Invite is invalid, expired, or already claimed")

    private fun notFound(): Nothing =
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

    private companion object {
        val INVITATION_TOKEN = Regex("^[a-f0-9]{64}$")
    }
}

private fun GroupEntity.toResponse() = GroupResponse(groupId, name, revision)
