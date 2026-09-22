package com.subhrodip.pennywise.expensecore.expenses.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/** One API allocation item. */
data class AllocationItemDto(
    @field:NotBlank val participantId: String,
    @field:NotBlank @field:Pattern(regexp = "^[0-9]+$") val value: String
)
