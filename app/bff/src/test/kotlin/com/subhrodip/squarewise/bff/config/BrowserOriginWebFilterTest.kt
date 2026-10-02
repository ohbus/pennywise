package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Verifies browser-origin enforcement and CORS response behavior at the WebFilter boundary. */
class BrowserOriginWebFilterTest {
    private val filter = BrowserOriginWebFilter(
        BrowserOriginProperties(listOf("https://app.example.test"))
    )

    @Test
    fun `unrelated paths pass through without origin processing`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/health").build())

        filter.filter(exchange, chain(reached)).block()

        assertTrue(reached.get())
        assertEquals(null, exchange.response.statusCode)
    }

    @Test
    fun `browser authentication without origin is rejected`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(ApiEndpoints.Bff.BROWSER_LOGIN_START).build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertFalse(reached.get())
        assertEquals(HttpStatus.FORBIDDEN, exchange.response.statusCode)
    }

    @Test
    fun `disallowed GraphQL origin is rejected`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(ApiEndpoints.Bff.GRAPHQL)
                .header(HttpHeaders.ORIGIN, "https://evil.example.test")
                .build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertFalse(reached.get())
        assertEquals(HttpStatus.FORBIDDEN, exchange.response.statusCode)
    }

    @Test
    fun `allowed GraphQL origin passes with credentialed CORS headers`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(ApiEndpoints.Bff.GRAPHQL)
                .header(HttpHeaders.ORIGIN, "https://app.example.test")
                .build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertTrue(reached.get())
        assertEquals("https://app.example.test", exchange.response.headers.accessControlAllowOrigin)
        assertEquals(false, exchange.response.headers.accessControlAllowCredentials)
        assertEquals("Origin", exchange.response.headers.getFirst(HttpHeaders.VARY))
    }

    @Test
    fun `allowed browser-auth preflight returns no content and permitted headers`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.options(ApiEndpoints.Bff.BROWSER_LOGIN_VERIFY)
                .header(HttpHeaders.ORIGIN, "https://app.example.test")
                .build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertFalse(reached.get())
        assertEquals(HttpStatus.NO_CONTENT, exchange.response.statusCode)
        assertEquals(true, exchange.response.headers.accessControlAllowCredentials)
        assertTrue(exchange.response.headers.accessControlAllowMethods.contains(HttpMethod.OPTIONS))
        assertTrue(exchange.response.headers.accessControlAllowHeaders.contains(HttpHeaders.CONTENT_TYPE))
    }

    private fun chain(reached: AtomicBoolean): WebFilterChain = WebFilterChain {
        reached.set(true)
        Mono.empty()
    }
}
