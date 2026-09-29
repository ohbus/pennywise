package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Enforces a double-submit CSRF nonce for all cookie-backed state-changing requests.
 *
 * Covered paths:
 * - Browser token refresh and logout endpoints (always require CSRF).
 * - POST to the GraphQL endpoint when the access cookie is present but no explicit
 *   `Authorization` header is supplied (cookie-authenticated mutations).
 *
 * Pure bearer-authenticated GraphQL requests (native/CLI clients that send an explicit
 * `Authorization` header without the access cookie) are NOT subject to CSRF checks.
 * GraphQL queries and non-POST requests are also unaffected.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class BrowserCsrfWebFilter(
    private val properties: BrowserSessionProperties
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()

        if (request.method != HttpMethod.POST) {
            return chain.filter(exchange)
        }

        val requiresCsrf = when {
            path == ApiEndpoints.Bff.BROWSER_TOKEN_REFRESH || path == ApiEndpoints.Bff.BROWSER_LOGOUT -> true
            path == ApiEndpoints.Bff.GRAPHQL && isCookieAuthenticated(exchange) -> true
            else -> false
        }

        if (!requiresCsrf) {
            return chain.filter(exchange)
        }

        val cookie = request.cookies.getFirst(properties.csrfCookieName)?.value
        val header = request.headers.getFirst(ApiEndpoints.Headers.X_CSRF_TOKEN)
        if (cookie.isNullOrBlank() || header.isNullOrBlank() || !sameValue(cookie, header)) {
            exchange.response.statusCode = HttpStatus.FORBIDDEN
            return exchange.response.setComplete()
        }
        return chain.filter(exchange)
    }

    /**
     * Returns true when the request carries the access cookie but no explicit
     * `Authorization` header, indicating a browser cookie-authenticated session.
     *
     * Note: this filter runs at HIGHEST_PRECEDENCE+1, *before* [BrowserAccessCookieWebFilter]
     * (HIGHEST_PRECEDENCE+2), so the Authorization header has not yet been injected from
     * the cookie at this point. We therefore inspect the raw cookie directly.
     */
    private fun isCookieAuthenticated(exchange: ServerWebExchange): Boolean =
        exchange.request.cookies.getFirst(properties.accessCookieName) != null &&
            exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) == null

    private fun sameValue(left: String, right: String): Boolean = MessageDigest.isEqual(
        left.toByteArray(StandardCharsets.UTF_8),
        right.toByteArray(StandardCharsets.UTF_8)
    )
}
