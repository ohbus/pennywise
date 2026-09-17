package com.subhrodip.pennywise.expensecore.expenses

/**
 * Mapping functions to transform persistence entities to domain models.
 */

/**
 * Maps an [ExpenseEntity] to its domain [ExpenseRecord] representation.
 */
fun ExpenseEntity.toRecord(): ExpenseRecord = ExpenseRecord(
    expenseId = expenseId,
    groupId = groupId,
    description = description,
    category = category,
    currency = currency,
    amountMinor = amountMinor,
    version = version,
    allocationMode = allocationMode,
    createdAt = createdAt,
    payers = payers.map { ExpensePayer(it.participantId, it.amountMinor) },
    allocations = allocations.map { ExpenseAllocation(it.participantId, it.allocatedMinor) },
    deleted = deleted,
    updatedAt = updatedAt
)
