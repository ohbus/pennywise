package com.subhrodip.pennywise.expensecore.settlements.api

import jakarta.validation.constraints.Pattern
import java.util.UUID

/** Transport input for recording a settlement between two group participants. */
data class RecordSettlementRequest(
    val fromParticipantId: UUID,
    val toParticipantId: UUID,
    @field:Pattern(regexp = "^[0-9]+$") val amountMinor: String
)
