package com.subhrodip.pennywise.bff

import com.subhrodip.pennywise.bff.config.GraphQlAbuseProperties
import com.subhrodip.pennywise.bff.realtime.LiveUpdateFanout
import com.subhrodip.pennywise.ids.contracts.ApiEndpoints
import com.subhrodip.pennywise.security.OidcConfigurationGuard
import java.time.Duration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono

/** Root Spring Boot composition for the GraphQL BFF application. */
@SpringBootApplication
@Import(OidcConfigurationGuard::class)
class BffApplication {
    @Bean
    fun liveUpdateFanout(properties: GraphQlAbuseProperties): LiveUpdateFanout = LiveUpdateFanout(
        queueCapacity = properties.subscriptionQueueCapacity,
        subscriptionTtl = Duration.ofSeconds(properties.subscriptionTtlSeconds),
        maxSubscriptionsPerUser = properties.maxSubscriptionsPerUser
    )

    /** Returns the deterministic GraphQL failure envelope used by live acceptance. */
    @Bean
    fun acceptanceFaultResponse(): WebFilter = WebFilter { exchange, chain ->
        val request = exchange.request
        if (request.uri.path == ApiEndpoints.Bff.GRAPHQL && request.headers.getFirst(ApiEndpoints.Headers.ACCEPTANCE_FAULT) == ApiEndpoints.Bff.ACCEPTANCE_FAULT_FANOUT) {
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

/** Starts the GraphQL BFF Spring Boot application for IDE and command-line launches. */
fun main(args: Array<String>) {
    runApplication<BffApplication>(*args)
}
