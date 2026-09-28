package com.subhrodip.squarewise.bff.transport.model.output

/** Settlement representation exposed by the BFF. */
data class BffSettlement(val id: String, val from: String? = null, val to: String? = null, val amountMinor: Long? = null, val status: String, val currency: String) { val amount: BffMoney get() = BffMoney(currency, (amountMinor ?: 0L).toString()) }
