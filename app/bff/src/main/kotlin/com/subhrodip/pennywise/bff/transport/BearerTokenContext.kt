package com.subhrodip.pennywise.bff.transport

/** Stable Reactor-context key used to forward the inbound opaque bearer token. */
internal object BearerTokenContext {
    const val KEY: String = "pennywise.bff.bearer-token"
}
