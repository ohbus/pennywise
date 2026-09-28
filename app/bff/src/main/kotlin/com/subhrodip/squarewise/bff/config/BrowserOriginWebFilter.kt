package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.core.annotation.Order
import org.springframework.core.Ordered
import reactor.core.publisher.Mono

/** Applies exact-origin and non-credentialed CORS rules to the GraphQL boundary. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class BrowserOriginWebFilter(properties: BrowserOriginProperties) : WebFilter {
    private val policy = BrowserOriginPolicy(properties.allowedOrigins)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (exchange.request.path.value() != ApiEndpoints.Bff.GRAPHQL) return chain.filter(exchange)

        val origin = exchange.request.headers.getFirst(HttpHeaders.ORIGIN)
        if (!policy.allows(origin)) {
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }
        if (origin == null) return chain.filter(exchange)

        exchange.response.headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN)
        exchange.response.headers.accessControlAllowOrigin = origin
        if (exchange.request.method == HttpMethod.OPTIONS) {
            exchange.response.headers.accessControlAllowMethods = listOf(
                HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS
            )
            exchange.response.headers.accessControlAllowHeaders = listOf(
                HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT
            )
            exchange.response.statusCode = HttpStatus.NO_CONTENT
            return exchange.response.setComplete()
        }
        return chain.filter(exchange)
    }
}
