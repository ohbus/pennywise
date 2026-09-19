package com.subhrodip.pennywise.accounts.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector
import org.springframework.security.web.SecurityFilterChain
import java.security.Principal

/**
 * Local-profile security configuration for Accounts that uses Spring's oauth2ResourceServer
 * with a passthrough [OpaqueTokenIntrospector] that accepts any Bearer token and uses it
 * literally as the principal name (sub claim).
 *
 * This configuration is **only active under the `local` Spring profile** and must never be
 * deployed to production. It enables the acceptance smoke suite without a real OIDC provider.
 */
@Configuration
@Profile("local")
@EnableWebSecurity
class LocalSecurityConfig {

    @Bean
    fun localSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated() }
            .oauth2ResourceServer { it.opaqueToken { token -> token.introspector(localTokenIntrospector()) } }
        return http.build()
    }

    @Bean
    fun localTokenIntrospector(): OpaqueTokenIntrospector = LocalPassthroughTokenIntrospector()
}

/**
 * Opaque token introspector for local/demo use that treats the raw Bearer token value as
 * the authenticated subject (principal name) without any network call or signature validation.
 *
 * Returns a [LocalUserPrincipal] that implements both [OAuth2AuthenticatedPrincipal] and
 * [java.security.Principal] so it can be directly injected into controller method parameters
 * annotated with `@AuthenticationPrincipal principal: Principal`.
 */
class LocalPassthroughTokenIntrospector : OpaqueTokenIntrospector {
    override fun introspect(token: String): OAuth2AuthenticatedPrincipal = LocalUserPrincipal(token)
}

/**
 * Principal for local demo mode that implements both [OAuth2AuthenticatedPrincipal] and
 * [java.security.Principal]. This dual interface ensures correct injection into controller
 * parameters typed as either [OAuth2AuthenticatedPrincipal] or `java.security.Principal`.
 */
class LocalUserPrincipal(private val subject: String) : OAuth2AuthenticatedPrincipal, Principal {
    override fun getName(): String = subject
    override fun getAttributes(): Map<String, Any> =
        mapOf(OAuth2TokenIntrospectionClaimNames.SUB to subject)
    override fun getAuthorities(): Collection<SimpleGrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_USER"))
}
