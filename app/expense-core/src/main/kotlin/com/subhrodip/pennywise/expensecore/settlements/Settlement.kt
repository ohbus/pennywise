package com.subhrodip.pennywise.expensecore.settlements

import java.util.UUID

/**
 * Domain entity representing a financial settlement payment between group participants.
 */
data class Settlement(
    val id: UUID,
    val fromParticipantId: UUID,
    val toParticipantId: UUID,
    val amountMinor: Long,
    val currency: String = "EUR",
    val reason: String? = null,
    val status: SettlementStatus = SettlementStatus.RECORDED
)
