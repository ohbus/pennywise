package com.subhrodip.pennywise.bff.transport.model.output

/** Account profile representation exposed through the BFF. */
data class BffProfile(val accountId: String, val displayName: String, val timezone: String, val defaultCurrency: String)
