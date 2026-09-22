package com.subhrodip.pennywise.expensecore.recurring.api
import com.subhrodip.pennywise.expensecore.expenses.api.request.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/** Participant allocation in a recurring schedule request. */
data class ExpenseAllocationItemDto(@field:NotBlank val participantId: String, @field:Valid val amount: MoneyDto)
