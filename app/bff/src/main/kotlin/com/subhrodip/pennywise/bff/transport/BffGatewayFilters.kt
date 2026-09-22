package com.subhrodip.pennywise.bff.transport

import com.subhrodip.pennywise.db.routing.DbWatermark
import com.subhrodip.pennywise.db.routing.DbWatermarkHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/** Shared request propagation filter used by BFF upstream gateways. */
object BffGatewayFilters {
    val bearerPropagation: ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
        Mono.deferContextual { context ->
            val token: String? = context.getOrDefault(BearerTokenContext.KEY, null as String?)
            val watermark: String? = context.getOrDefault(BearerTokenContext.WATERMARK_KEY, null as String?)
            val exchange: ServerWebExchange? = context.getOrDefault(BearerTokenContext.EXCHANGE_KEY, null as ServerWebExchange?)
            val forwardedRequest = ClientRequest.from(request).headers { headers ->
                if (token is String && token.isNotEmpty()) headers.setBearerAuth(token)
                if (watermark is String && watermark.isNotEmpty()) headers.set(DbWatermarkHeaders.REQUIRED_WATERMARK, watermark)
            }.build()
            next.exchange(forwardedRequest).doOnNext { response ->
                val downstream = response.headers().header(DbWatermarkHeaders.WRITER_WATERMARK).firstOrNull()
                    ?.let { runCatching { DbWatermark.parse(it).asLsn() }.getOrNull() }
                if (exchange != null && downstream != null) {
                    val current = exchange.response.headers.getFirst(DbWatermarkHeaders.WRITER_WATERMARK)
                    val existing = current?.let { runCatching { DbWatermark.parse(it).asLsn() }.getOrNull() }
                    if (existing == null || downstream > existing) {
                        exchange.response.headers.set(DbWatermarkHeaders.WRITER_WATERMARK, downstream)
                    }
                }
            }
        }
    }
}
