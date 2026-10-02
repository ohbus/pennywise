package com.subhrodip.squarewise.bff

import com.subhrodip.squarewise.ids.contracts.ApiEndpoints
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Verifies the BFF acceptance-fault filter at its HTTP boundary.
 *
 * The acceptance hook must emit the deterministic fanout fault only for the
 * GraphQL path and matching header, while ordinary requests must continue to
 * the downstream chain unchanged.
 */
class BffApplicationTest {

    private val filter = BffApplication().acceptanceFaultResponse()

    @Test
    fun `matching GraphQL fanout fault returns deterministic JSON failure`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(ApiEndpoints.Bff.GRAPHQL)
                .header(ApiEndpoints.Headers.ACCEPTANCE_FAULT, ApiEndpoints.Bff.ACCEPTANCE_FAULT_FANOUT)
                .build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertFalse(reached.get())
        assertEquals(HttpStatus.OK, exchange.response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, exchange.response.headers.contentType)
    }

    @Test
    fun `non-matching request passes through to downstream chain`() {
        val reached = AtomicBoolean(false)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post(ApiEndpoints.Bff.GRAPHQL)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
        )

        filter.filter(exchange, chain(reached)).block()

        assertTrue(reached.get())
        assertEquals(null, exchange.response.statusCode)
    }

    private fun chain(reached: AtomicBoolean): WebFilterChain = WebFilterChain {
        reached.set(true)
        Mono.empty()
    }
}
