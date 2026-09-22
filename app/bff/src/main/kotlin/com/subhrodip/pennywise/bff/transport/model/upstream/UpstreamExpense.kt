package com.subhrodip.pennywise.bff.transport.model.upstream

import com.subhrodip.pennywise.bff.transport.model.output.BffAllocation
import com.subhrodip.pennywise.bff.transport.model.output.BffExpense
import com.subhrodip.pennywise.bff.transport.model.output.BffMoney
/** Expense response shape received from Expense Core before BFF normalization. */
internal data class UpstreamExpense(
    val expenseId: String,
    val version: Long = 1,
    val description: String? = null,
    val amount: BffMoney,
    val category: String? = null,
    val allocations: List<BffAllocation> = emptyList()
) {
    fun toBffExpense(fallbackDescription: String = ""): BffExpense = BffExpense(
        expenseId = expenseId,
        version = version,
        description = description ?: fallbackDescription,
        amount = amount,
        category = category,
        allocations = allocations
    )
}
