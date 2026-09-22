package com.subhrodip.pennywise.expensecore.expenses.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Durable claim linking one expense mutation key to its committed response. */
@Entity
@Table(name = "expense_idempotency")
class ExpenseIdempotencyEntity(
    @Id
    @Column(name = "idempotency_id", nullable = false)
    var idempotencyId: UUID,
    @Column(name = "group_id", nullable = false)
    var groupId: UUID,
    @Column(name = "actor_subject", nullable = false, length = 255)
    var actorSubject: String,
    @Column(name = "operation", nullable = false, length = 64)
    var operation: String,
    @Column(name = "idempotency_key", nullable = false, length = 255)
    var idempotencyKey: String,
    @Column(name = "payload_hash", nullable = false, length = 64)
    var payloadHash: String,
    @Column(name = "expense_id", nullable = false)
    var expenseId: UUID,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
