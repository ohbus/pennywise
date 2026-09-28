package com.subhrodip.pennywise.expensecore.expenses.domain

import java.time.Instant
import java.util.UUID

/** Internal expense aggregate snapshot used by stores and posting logic. */
data class ExpenseRecord(
    val expenseId: UUID,
    val groupId: UUID,
    val description: String,
    val category: String,
    val currency: String,
    val amountMinor: Long,
    val version: Long,
    val allocationMode: String,
    val createdAt: Instant,
    val payers: List<ExpensePayer>,
    val allocations: List<ExpenseAllocation>,
    val deleted: Boolean = false,
    val updatedAt: Instant? = null
)
