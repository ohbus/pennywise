package com.subhrodip.pennywise.bff.config

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

import com.subhrodip.pennywise.bff.transport.BearerTokenContext
import com.subhrodip.pennywise.db.routing.DbWatermark
import com.subhrodip.pennywise.db.routing.DbWatermarkHeaders
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private const val BEARER_SCHEME = "${ApiEndpoints.Headers.BEARER_SCHEME} "

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
        val watermark = exchange.request.headers.getFirst(DbWatermarkHeaders.REQUIRED_WATERMARK)
            ?.let { candidate -> runCatching { DbWatermark.parse(candidate).asLsn() }.getOrNull() }
        return chain.filter(exchange).contextWrite { context ->
            var updated = context
            if (token != null) updated = updated.put(BearerTokenContext.KEY, token)
            if (watermark != null) updated = updated.put(BearerTokenContext.WATERMARK_KEY, watermark)
            updated = updated.put(BearerTokenContext.EXCHANGE_KEY, exchange)
            updated
        }
    }
}
