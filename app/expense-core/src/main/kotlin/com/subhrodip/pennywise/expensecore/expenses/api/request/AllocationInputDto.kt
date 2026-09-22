package com.subhrodip.pennywise.expensecore.expenses.api.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

/** API allocation strategy and its participant items. */
data class AllocationInputDto(
    @field:NotBlank @field:Pattern(regexp = "^(EQUAL|EXACT|PERCENT_BASIS_POINTS|WEIGHTED_SHARES)$") val mode: String,
    @field:NotEmpty val items: List<@Valid AllocationItemDto>
)
