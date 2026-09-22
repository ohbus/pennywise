package com.subhrodip.pennywise.bff.config

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

import com.subhrodip.pennywise.security.ReactiveOidcJwtDecoderFactory
import com.subhrodip.pennywise.security.OidcSecurityConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain

/** Production/staging reactive JWT security chain for the GraphQL BFF. */
@Configuration
@Profile("production", "staging", "local-oidc")
class ProductionSecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @Value("\${pennywise.security.oidc.audience}") private val audience: String,
    @Value("\${pennywise.security.oidc.allowed-algorithms:}") private val allowedAlgorithms: String
) {
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder =
        ReactiveOidcJwtDecoderFactory.create(
            issuerUri, audience, OidcSecurityConstants.configuredSigningAlgorithms(allowedAlgorithms)
        )

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
        .csrf { it.disable() }
        .authorizeExchange { it.pathMatchers(ApiEndpoints.Operations.HEALTH).permitAll().anyExchange().authenticated() }
        .oauth2ResourceServer { it.jwt {} }
        .build()
}
