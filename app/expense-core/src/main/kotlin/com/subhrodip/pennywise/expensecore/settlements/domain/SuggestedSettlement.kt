package com.subhrodip.pennywise.expensecore.settlements.domain

import java.util.UUID

/** A recommended transfer that reduces outstanding group balances. */
data class SuggestedSettlement(
    val fromParticipantId: UUID,
    val toParticipantId: UUID,
    val amountMinor: Long,
    val currency: String
)
