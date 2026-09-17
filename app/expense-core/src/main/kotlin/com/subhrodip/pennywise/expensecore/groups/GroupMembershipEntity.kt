package com.subhrodip.pennywise.expensecore.groups

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * Persistent JPA entity associating a subject user to an expense group membership.
 *
 * Invariants:
 * - [membershipId] is a unique primary key UUID.
 * - The pair ([groupId], [subject]) is constrained to be unique in the database schema.
 * - [subject] is the authenticated principal identifier (up to 200 characters).
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
    @Column(name = "subject", nullable = false, length = 200)
    var subject: String
)
