package com.subhrodip.pennywise.expensecore.expenses

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity representing a balance posting entry in the double-entry ledger.
 *
 * Each posting credits or debits a participant's balance within a group for a specific currency.
 */
@Entity
@Table(name = "balance_postings")
class BalancePostingEntity(
    @Id
    @Column(name = "posting_id", nullable = false)
    var postingId: UUID,

    @Column(name = "group_id", nullable = false)
    var groupId: UUID,

    @Column(name = "expense_id")
    var expenseId: UUID? = null,

    @Column(name = "settlement_id")
    var settlementId: UUID? = null,

    @Column(name = "participant_id", nullable = false)
    var participantId: UUID,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "amount_minor", nullable = false)
    var amountMinor: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
