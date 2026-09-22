package com.subhrodip.pennywise.bff.transport.model.output

/** Suggested settlement representation exposed by the BFF. */
data class BffSuggestedSettlement(val fromParticipantId: String, val toParticipantId: String, val amountMinor: Long, val currency: String) { val amount: BffMoney get() = BffMoney(currency, amountMinor.toString()) }
