package com.subhrodip.squarewise.bff.transport

/** Stable Reactor-context key used to forward the inbound opaque bearer token. */
internal object BearerTokenContext {
    const val KEY: String = "squarewise.bff.bearer-token"
    const val WATERMARK_KEY: String = "squarewise.bff.required-watermark"
    const val EXCHANGE_KEY: String = "squarewise.bff.server-exchange"
}
