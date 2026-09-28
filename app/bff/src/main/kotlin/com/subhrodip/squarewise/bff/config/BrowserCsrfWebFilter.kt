package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Enforces a double-submit CSRF nonce for cookie-backed session mutations. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class BrowserCsrfWebFilter(
    private val properties: BrowserSessionProperties
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        if (exchange.request.method != HttpMethod.POST ||
            (path != ApiEndpoints.Bff.BROWSER_TOKEN_REFRESH && path != ApiEndpoints.Bff.BROWSER_LOGOUT)
        ) {
            return chain.filter(exchange)
        }

        val cookie = exchange.request.cookies.getFirst(properties.csrfCookieName)?.value
        val header = exchange.request.headers.getFirst(ApiEndpoints.Headers.X_CSRF_TOKEN)
        if (cookie.isNullOrBlank() || header.isNullOrBlank() || !sameValue(cookie, header)) {
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }
        return chain.filter(exchange)
    }

    private fun sameValue(left: String, right: String): Boolean = MessageDigest.isEqual(
        left.toByteArray(StandardCharsets.UTF_8),
        right.toByteArray(StandardCharsets.UTF_8)
    )
}
