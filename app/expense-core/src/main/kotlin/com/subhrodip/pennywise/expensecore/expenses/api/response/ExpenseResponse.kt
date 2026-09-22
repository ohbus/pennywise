package com.subhrodip.pennywise.expensecore.expenses.api.response

import com.subhrodip.pennywise.expensecore.expenses.api.request.MoneyDto
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

/** Public expense representation returned by Expense Core. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseResponse(val expenseId: UUID, val version: Long, val amount: MoneyDto, val category: String, val allocations: List<ExpenseAllocationResponse>)
