package com.subhrodip.pennywise.notifications

import com.subhrodip.pennywise.security.OidcJwtDecoderFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

/** Production/staging JWT security chain for Notifications. */
@Configuration
@Profile("production", "staging")
@EnableWebSecurity
class ProductionSecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @Value("\${pennywise.security.oidc.audience}") private val audience: String
) {
    @Bean
    fun jwtDecoder(): JwtDecoder = OidcJwtDecoderFactory.create(issuerUri, audience)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { it.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated() }
        .oauth2ResourceServer { it.jwt {} }
        .build()
}
