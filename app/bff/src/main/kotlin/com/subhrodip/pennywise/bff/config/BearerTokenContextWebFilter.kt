package com.subhrodip.pennywise.bff.config

import com.subhrodip.pennywise.bff.transport.BearerTokenContext
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private const val BEARER_SCHEME = "Bearer "

/** Captures the inbound bearer token at the HTTP boundary for reactive fan-out. */
@Component
class BearerTokenContextWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authorization = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val token = authorization
            ?.takeIf { it.startsWith(BEARER_SCHEME, ignoreCase = true) }
            ?.substring(BEARER_SCHEME.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return chain.filter(exchange).contextWrite { context ->
            if (token == null) context else context.put(BearerTokenContext.KEY, token)
        }
    }
}
