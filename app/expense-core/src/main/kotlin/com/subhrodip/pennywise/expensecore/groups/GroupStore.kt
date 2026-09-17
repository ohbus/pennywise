package com.subhrodip.pennywise.expensecore.groups

import java.util.UUID

/**
 * Domain port defining persistence operations for expense groups, memberships, and invitations.
 */
interface GroupStore {
    /**
     * Creates a new group owned by the creator.
     */
    fun create(subject: String, request: CreateGroupRequest): GroupResponse

    /**
     * Lists all groups in which the subject holds an active membership.
     */
    fun list(subject: String): List<GroupResponse>

    /**
     * Updates an existing group's metadata (e.g. name), incrementing the group revision.
     */
    fun update(groupId: UUID, subject: String, request: UpdateGroupRequest): GroupResponse

    /**
     * Lists all member subjects belonging to the specified group.
     */
    fun listMembers(groupId: UUID, subject: String): List<GroupMemberResponse>

    /**
     * Generates a time-limited invitation token for joining a group.
     */
    fun invite(groupId: UUID, subject: String, request: CreateInviteRequest): InviteResponse

    /**
     * Claims an active invitation token, adding the caller to the target group.
     */
    fun claim(token: String, subject: String): GroupResponse
}
