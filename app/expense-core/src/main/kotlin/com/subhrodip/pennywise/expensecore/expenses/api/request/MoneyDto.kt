package com.subhrodip.pennywise.expensecore.expenses.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/** Validated API money value represented in minor units. */
data class MoneyDto(
    @field:NotBlank @field:Pattern(regexp = "^[A-Z]{3}$") val currency: String,
    @field:NotBlank @field:Pattern(regexp = "^-?[0-9]+$") val minor: String
)
