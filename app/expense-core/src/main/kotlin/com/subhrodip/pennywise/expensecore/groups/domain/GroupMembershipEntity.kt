package com.subhrodip.pennywise.expensecore.groups.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * Persistent JPA entity associating a user subject or placeholder to an expense group membership.
 *
 * Invariants:
 * - [membershipId] is a unique primary key UUID used as the participant identifier across expenses and ledger entries.
 * - The pair ([groupId], [subject]) is constrained to be unique in the database schema when subject is non-null.
 * - [subject] is the authenticated principal identifier (nullable for unclaimed placeholders).
 * - [displayName] is an optional human-readable label for placeholders or members.
 * - [isPlaceholder] is true for members created without a bound user account.
 * - [status] is either ACTIVE or REMOVED.
 */
@Entity
@Table(
    name = "group_memberships",
    uniqueConstraints = [UniqueConstraint(
        name = "group_memberships_group_subject_key",
        columnNames = ["group_id", "subject"]
    )]
)
class GroupMembershipEntity(
    @Id
    @Column(name = "membership_id", nullable = false)
    var membershipId: UUID,
    @Column(name = "group_id", nullable = false)
    var groupId: UUID,
    @Column(name = "subject", length = 200)
    var subject: String? = null,
    @Column(name = "display_name", length = 120)
    var displayName: String? = null,
    @Column(name = "is_placeholder", nullable = false)
    var isPlaceholder: Boolean = false,
    @Column(name = "status", nullable = false, length = 16)
    var status: String = "ACTIVE"
)
