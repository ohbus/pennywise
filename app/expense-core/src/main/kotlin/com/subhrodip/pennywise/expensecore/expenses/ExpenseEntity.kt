package com.subhrodip.pennywise.expensecore.expenses

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * JPA entity representing an expense header record within a group.
 *
 * Tracks top-level financial details, allocation mode, optimistic concurrency version,
 * and associated payers and allocations.
 */
@Entity
@Table(name = "expenses")
class ExpenseEntity(
    @Id
    @Column(name = "expense_id", nullable = false)
    var expenseId: UUID,

    @Column(name = "group_id", nullable = false)
    var groupId: UUID,

    @Column(name = "description", nullable = false, length = 240)
    var description: String,

    @Column(name = "category", nullable = false, length = 32)
    var category: String = "other",

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "amount_minor", nullable = false)
    var amountMinor: Long,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 1,

    @Column(name = "allocation_mode", nullable = false, length = 32)
    var allocationMode: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var payers: MutableList<ExpensePayerEntity> = mutableListOf(),

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var allocations: MutableList<ExpenseAllocationEntity> = mutableListOf()
)
