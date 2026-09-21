package com.subhrodip.pennywise.bff.transport

/** Stable Reactor-context key used to forward the inbound opaque bearer token. */
internal object BearerTokenContext {
    const val KEY: String = "pennywise.bff.bearer-token"
    const val WATERMARK_KEY: String = "pennywise.bff.required-watermark"
    const val EXCHANGE_KEY: String = "pennywise.bff.server-exchange"
}
