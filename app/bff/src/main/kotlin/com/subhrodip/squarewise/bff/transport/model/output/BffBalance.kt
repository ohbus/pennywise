package com.subhrodip.squarewise.bff.transport.model.output

/** Participant balance returned by Expense Core. */
data class BffBalance(val participantId: String, val amount: BffMoney) { val money: BffMoney get() = amount }
