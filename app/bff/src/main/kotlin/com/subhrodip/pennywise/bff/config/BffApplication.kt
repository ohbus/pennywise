package com.subhrodip.pennywise.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono
import com.subhrodip.pennywise.security.OidcConfigurationGuard

@SpringBootApplication
@Import(OidcConfigurationGuard::class)
class BffApplication {
    @Bean
    fun liveUpdateFanout(): LiveUpdateFanout = LiveUpdateFanout()

    /** Returns the deterministic GraphQL failure envelope used by live acceptance. */
    @Bean
    fun acceptanceFaultResponse(): WebFilter = WebFilter { exchange, chain ->
        val request = exchange.request
        if (request.uri.path == "/graphql" && request.headers.getFirst("X-Acceptance-Fault") == "fanout") {
            val response = exchange.response
            response.statusCode = HttpStatus.OK
            response.headers.contentType = MediaType.APPLICATION_JSON
            val body = "{\"data\":null,\"errors\":[{\"message\":\"Upstream fanout unavailable\"}]}"
            Mono.just(response.bufferFactory().wrap(body.toByteArray(Charsets.UTF_8)))
                .let(response::writeWith)
        } else {
            chain.filter(exchange)
        }
    }
}

fun main(args: Array<String>) = runApplication<BffApplication>(*args)
