package com.subhrodip.squarewise.expensecore.expenses.api.response

import com.subhrodip.squarewise.expensecore.expenses.api.request.MoneyDto
/** API allocation result for an expense participant. */
data class ExpenseAllocationResponse(val participantId: String, val amount: MoneyDto)
