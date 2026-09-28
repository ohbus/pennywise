package com.subhrodip.pennywise.bff.transport.model.upstream

import com.subhrodip.pennywise.bff.transport.model.output.BffSettlement
/** Settlement response shape received from Expense Core. */
internal data class UpstreamSettlement(
    val id: String,
    val fromParticipantId: String? = null,
    val toParticipantId: String? = null,
    val amountMinor: Long? = null,
    val status: String = "RECORDED"
) {
    fun toBffSettlement(currency: String): BffSettlement = BffSettlement(
        id = id,
        from = fromParticipantId,
        to = toParticipantId,
        amountMinor = amountMinor,
        status = status,
        currency = currency
    )
}
