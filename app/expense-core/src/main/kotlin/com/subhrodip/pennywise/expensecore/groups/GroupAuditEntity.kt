package com.subhrodip.pennywise.expensecore.groups

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Persistent JPA entity logging audit records for group modifications and lifecycle changes.
 *
 * Invariants:
 * - [auditId] is a unique primary key UUID.
 * - [groupId] references the target group being modified.
 * - [subject] identifies the user/actor who executed the modification.
 * - [action] describes the specific mutation action performed (e.g., "group.renamed").
 * - [revision] records the resulting group revision counter at the time of the action.
 * - [payload] contains the serialized JSON payload capturing the event delta.
 * - [occurredAt] records the timestamp when the audited event occurred.
 */
@Entity
@Table(name = "group_audit")
class GroupAuditEntity(
    @Id
    @Column(name = "audit_id", nullable = false)
    var auditId: UUID,
    @Column(name = "group_id", nullable = false)
    var groupId: UUID,
    @Column(name = "subject", nullable = false, length = 200)
    var subject: String,
    @Column(name = "action", nullable = false, length = 80)
    var action: String,
    @Column(name = "revision", nullable = false)
    var revision: Long,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant
)
