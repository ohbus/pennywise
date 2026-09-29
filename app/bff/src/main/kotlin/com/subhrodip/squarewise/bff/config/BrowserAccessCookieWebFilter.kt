package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Adapts only the BFF access cookie into the existing bearer security boundary. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
class BrowserAccessCookieWebFilter(
    private val properties: BrowserSessionProperties
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange.request.path.value() != ApiEndpoints.Bff.GRAPHQL ||
            exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) != null
        ) {
            return chain.filter(exchange)
        }
        val access = exchange.request.cookies.getFirst(properties.accessCookieName)?.value
            ?: return chain.filter(exchange)
        val request = exchange.request.mutate()
            .header(HttpHeaders.AUTHORIZATION, "${ApiEndpoints.Headers.BEARER_SCHEME} $access")
            .build()
        return chain.filter(exchange.mutate().request(request).build())
    }
}
