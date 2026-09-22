package com.subhrodip.pennywise.expensecore.recurring.api
import com.subhrodip.pennywise.expensecore.expenses.api.request.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/** Payer amount in a recurring schedule request. */
data class ExpensePayerDto(@field:NotBlank val participantId: String, @field:Valid val amount: MoneyDto)
