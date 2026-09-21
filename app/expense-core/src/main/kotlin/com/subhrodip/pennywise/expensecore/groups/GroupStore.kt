package com.subhrodip.pennywise.expensecore.groups

import java.util.UUID

/**
 * Domain port defining persistence operations for expense groups, memberships, placeholders, and invitations.
 */
/**
 * Command-side group and membership persistence port.
 */
interface GroupCommandStore {
    /**
     * Creates a new group owned by the creator.
     */
    fun create(subject: String, request: CreateGroupRequest): GroupResponse

    /**
     * Updates an existing group's metadata (e.g. name), incrementing the group revision.
     */
    fun update(groupId: UUID, subject: String, request: UpdateGroupRequest): GroupResponse

    /**
     * Archives an active group, transitioning its status to ARCHIVED and incrementing revision.
     */
    fun archive(groupId: UUID, subject: String): GroupResponse

    /**
     * Creates a named placeholder participant within an active group.
     */
    fun addPlaceholder(groupId: UUID, subject: String, request: CreatePlaceholderRequest): GroupMemberResponse

    /**
     * Soft-removes a member from an active group while preserving historical financial records.
     */
    fun removeMember(groupId: UUID, subject: String, membershipId: UUID)

    /**
     * Generates a time-limited invitation token for joining a group (optionally targeting a placeholder).
     */
    fun invite(groupId: UUID, subject: String, request: CreateInviteRequest): InviteResponse

    /**
     * Revokes a pending invitation token within an active group.
     */
    fun revokeInvite(groupId: UUID, subject: String, token: String)

    /**
     * Claims an active invitation token, adding the caller or binding to a targeted placeholder.
     */
    fun claim(token: String, subject: String): GroupResponse
}

/**
 * Query-side group and membership persistence port.
 */
interface GroupQueryStore {
    /**
     * Lists all groups in which the subject holds an active membership.
     */
    fun list(subject: String): List<GroupResponse>

    /**
     * Lists all active members and placeholders belonging to the specified group.
     */
    fun listMembers(groupId: UUID, subject: String): List<GroupMemberResponse>
}

/**
 * Compatibility facade combining group command and query ports.
 */
interface GroupStore : GroupCommandStore, GroupQueryStore
