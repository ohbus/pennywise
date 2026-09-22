package com.subhrodip.pennywise.expensecore.expenses.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** Input for previewing an equal allocation across bounded participants. */
data class AllocationPreviewRequest(
    @field:NotBlank @field:Pattern(regexp = "^[0-9]+$") val totalMinor: String,
    @field:NotEmpty @field:Size(max = 100) val participantIds: List<@NotBlank String>
)
