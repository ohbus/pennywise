package com.subhrodip.pennywise.expensecore.groups

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Persistent JPA entity representing an expense group within the core domain.
 *
 * Tracks the group's unique identifier, human-readable name, domain classification kind,
 * default ISO 4217 currency, and monotonic revision sequence.
 *
 * Invariants:
 * - [groupId] is a non-null, immutable UUID identifying the group.
 * - [name] must not exceed 120 characters and cannot be blank.
 * - [kind] must correspond to an accepted group classification (e.g. HOUSEHOLD, COUPLE, TRIP).
 * - [currency] must be a valid 3-character uppercase ISO code.
 * - [revision] starts at 0 and increments with each mutation for optimistic locking and sync events.
 */
@Entity
@Table(name = "expense_groups")
class GroupEntity(
    @Id
    @Column(name = "group_id", nullable = false)
    var groupId: UUID,
    @Column(name = "name", nullable = false, length = 120)
    var name: String,
    @Column(name = "kind", nullable = false, length = 16)
    var kind: String,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "revision", nullable = false)
    var revision: Long = 0
)
