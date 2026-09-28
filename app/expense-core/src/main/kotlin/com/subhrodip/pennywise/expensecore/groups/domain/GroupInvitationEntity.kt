package com.subhrodip.pennywise.expensecore.groups.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Persistent JPA entity representing a shareable invitation token to join an expense group.
 *
 * Invariants:
 * - [token] is a 64-character hexadecimal unique token string acting as the primary key.
 * - [groupId] references the target group that the claimant will join.
 * - [expiresAt] indicates the timestamp after which the invitation cannot be claimed.
 * - [claimedAt] and [claimedBy] are null until successfully claimed by a user.
 * - [revokedAt] is set if the invitation is explicitly revoked prior to claiming.
 * - [placeholderId] references a specific placeholder membership to bind upon claiming, if targeted.
 */
@Entity
@Table(name = "group_invitations")
class GroupInvitationEntity(
    @Id
    @Column(name = "token", nullable = false, length = 64)
    var token: String,
    @Column(name = "group_id", nullable = false)
    var groupId: UUID,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "claimed_at")
    var claimedAt: Instant? = null,
    @Column(name = "claimed_by", length = 200)
    var claimedBy: String? = null,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
    @Column(name = "placeholder_id")
    var placeholderId: UUID? = null
)
