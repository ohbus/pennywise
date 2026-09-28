package com.subhrodip.pennywise.bff.transport.model.input

/** GraphQL input for expense creation. */
data class CreateExpenseInput(val expenseId: String, val description: String, val amount: MoneyInput, val payers: List<PayerInput>, val allocation: AllocationInput)
