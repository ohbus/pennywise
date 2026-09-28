package com.subhrodip.squarewise.expensecore.expenses.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/** API payer allocation input. */
data class PayerDto(@field:NotBlank val participantId: String, @field:Valid val amount: MoneyDto)
