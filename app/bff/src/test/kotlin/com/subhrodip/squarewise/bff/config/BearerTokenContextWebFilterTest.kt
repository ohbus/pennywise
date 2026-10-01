package com.subhrodip.squarewise.bff.config

import com.subhrodip.squarewise.bff.transport.BearerTokenContext
import com.subhrodip.squarewise.db.routing.DbWatermark
import com.subhrodip.squarewise.db.routing.DbWatermarkHeaders
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.ContextView

/** Verifies bearer and causal-watermark capture at the reactive HTTP boundary. */
class BearerTokenContextWebFilterTest {
    private val filter = BearerTokenContextWebFilter()

    @Test
    fun `captures case-insensitive trimmed bearer and valid required watermark`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/graphql")
                .header(HttpHeaders.AUTHORIZATION, "bEaReR   opaque-token  ")
                .header(DbWatermarkHeaders.REQUIRED_WATERMARK, "0/16B6C50")
                .build()
        )
        val captured = AtomicReference<ContextView>()
        val chain = contextCapturingChain(captured)

        filter.filter(exchange, chain).block()

        assertEquals("opaque-token", captured.get().get<String>(BearerTokenContext.KEY))
        assertEquals(
            DbWatermark.parse("0/16B6C50").asLsn(),
            captured.get().get<String>(BearerTokenContext.WATERMARK_KEY)
        )
        assertSame(exchange, captured.get().get<ServerWebExchange>(BearerTokenContext.EXCHANGE_KEY))
    }

    @Test
    fun `omits malformed authorization and watermark while retaining the exchange`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/graphql")
                .header(HttpHeaders.AUTHORIZATION, "Basic not-a-bearer")
                .header(DbWatermarkHeaders.REQUIRED_WATERMARK, "not-an-lsn")
                .build()
        )
        val captured = AtomicReference<ContextView>()

        filter.filter(exchange, contextCapturingChain(captured)).block()

        assertFalse(captured.get().hasKey(BearerTokenContext.KEY))
        assertFalse(captured.get().hasKey(BearerTokenContext.WATERMARK_KEY))
        assertSame(exchange, captured.get().get<ServerWebExchange>(BearerTokenContext.EXCHANGE_KEY))
    }

    private fun contextCapturingChain(captured: AtomicReference<ContextView>): WebFilterChain =
        WebFilterChain { _: ServerWebExchange ->
            Mono.deferContextual { context ->
                captured.set(context)
                Mono.empty()
            }
        }
}
