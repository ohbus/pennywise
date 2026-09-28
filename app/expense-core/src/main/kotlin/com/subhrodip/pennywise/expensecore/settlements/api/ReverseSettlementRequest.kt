package com.subhrodip.pennywise.expensecore.settlements.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Transport input describing why a recorded settlement is reversed. */
data class ReverseSettlementRequest(
    @field:NotBlank @field:Size(max = 240) val reason: String
)
