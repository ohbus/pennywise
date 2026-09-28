package com.subhrodip.squarewise.bff.config

import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Verifies the browser access cookie adapter at the BFF bearer boundary. */
class BrowserAccessCookieWebFilterTest {
    private val filter = BrowserAccessCookieWebFilter(BrowserSessionProperties())

    @Test
    fun `graphql access cookie becomes a bearer header`() {
        val captured = AtomicReference<ServerWebExchange>()
        val chain = WebFilterChain { exchange ->
            captured.set(exchange)
            Mono.empty()
        }
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/graphql")
                .cookie(HttpCookie("squarewise_access", "access-cookie"))
                .build()
        )

        filter.filter(exchange, chain).block()

        assertEquals(
            "Bearer access-cookie",
            captured.get().request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        )
    }

    @Test
    fun `explicit bearer and non-graphql requests are not replaced`() {
        val captured = AtomicReference<ServerWebExchange>()
        val chain = WebFilterChain { exchange ->
            captured.set(exchange)
            Mono.empty()
        }
        val explicit = MockServerWebExchange.from(
            MockServerHttpRequest.get("/graphql")
                .header(HttpHeaders.AUTHORIZATION, "Bearer explicit")
                .cookie(HttpCookie("squarewise_access", "cookie"))
                .build()
        )
        filter.filter(explicit, chain).block()
        assertEquals("Bearer explicit", captured.get().request.headers.getFirst(HttpHeaders.AUTHORIZATION))

        val nonGraphql = MockServerWebExchange.from(
            MockServerHttpRequest.get("/health")
                .cookie(HttpCookie("squarewise_access", "cookie"))
                .build()
        )
        filter.filter(nonGraphql, chain).block()
        assertNull(captured.get().request.headers.getFirst(HttpHeaders.AUTHORIZATION))
    }
}
