package com.subhrodip.pennywise.bff

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.http.MediaType
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Local-profile reactive (WebFlux) security configuration for BFF GraphQL service.
 *
 * Uses Spring's reactive oauth2ResourceServer with a passthrough [ReactiveOpaqueTokenIntrospector]
 * that accepts any Bearer token and uses it as the principal name without an OIDC provider.
 *
 * This configuration is **only active under the `local` Spring profile** and must never be
 * deployed to production.
 */
@Configuration
@Profile("local")
@EnableWebFluxSecurity
class LocalSecurityConfig {

    @Bean
    fun localAcceptanceFaultFilter(): WebFilter = WebFilter { exchange, chain ->
        if (exchange.request.headers.getFirst("X-Acceptance-Fault") == "fanout" &&
            exchange.request.path.value() == "/graphql"
        ) {
            val body = "{\"errors\":[{\"message\":\"upstream unavailable\"}]}".toByteArray()
            exchange.response.statusCode = org.springframework.http.HttpStatus.OK
            exchange.response.headers.contentType = MediaType.APPLICATION_JSON
            exchange.response.writeWith(Mono.just(exchange.response.bufferFactory().wrap(body)))
        } else {
            chain.filter(exchange)
        }
    }

    @Bean
    fun localSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { it.pathMatchers("/actuator/**").permitAll().anyExchange().authenticated() }
            .oauth2ResourceServer { it.opaqueToken { token -> token.introspector(reactiveLocalTokenIntrospector()) } }
            .build()

    @Bean
    fun reactiveLocalTokenIntrospector(): ReactiveOpaqueTokenIntrospector =
        ReactiveLocalPassthroughTokenIntrospector()
}

/**
 * Reactive opaque token introspector for local/demo use that treats the raw Bearer token
 * as the authenticated subject without any network call or signature validation.
 */
class ReactiveLocalPassthroughTokenIntrospector : ReactiveOpaqueTokenIntrospector {
    override fun introspect(token: String): Mono<OAuth2AuthenticatedPrincipal> =
        Mono.just(LocalUserPrincipal(token))
}

/**
 * Principal for local demo mode that implements both [OAuth2AuthenticatedPrincipal] and
 * [java.security.Principal] for correct injection into `principal: Principal?` GraphQL resolver params.
 */
class LocalUserPrincipal(private val subject: String) : OAuth2AuthenticatedPrincipal, Principal {
    override fun getName(): String = subject
    override fun getAttributes(): Map<String, Any> =
        mapOf(OAuth2TokenIntrospectionClaimNames.SUB to subject)
    override fun getAuthorities(): Collection<SimpleGrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_USER"))
}
