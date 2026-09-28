package com.subhrodip.pennywise.bff.transport.model.output

/** Expense representation exposed by the BFF. */
data class BffExpense(val expenseId: String, val version: Long = 1, val description: String = "", val amount: BffMoney, val category: String? = null, val allocations: List<BffAllocation> = emptyList()) { val id: String get() = expenseId }
