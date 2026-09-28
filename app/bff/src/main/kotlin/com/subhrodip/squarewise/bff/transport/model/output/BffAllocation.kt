package com.subhrodip.squarewise.bff.transport.model.output

/** Expense allocation returned by the BFF. */
data class BffAllocation(val participantId: String, val amount: BffMoney)
