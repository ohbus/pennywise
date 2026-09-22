package com.subhrodip.pennywise.bff.transport.model.output

/** Group balances response exposed by the BFF. */
data class BffBalancesResponse(val groupId: String, val balances: List<BffBalance> = emptyList())
