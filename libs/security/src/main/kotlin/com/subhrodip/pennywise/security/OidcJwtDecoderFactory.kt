package com.subhrodip.pennywise.security

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

/** Builds a standards-based JWT decoder for the configured OIDC issuer. */
object OidcJwtDecoderFactory {
    /**
     * Creates a decoder using issuer-discovered keys and validators for issuer,
     * expiry, not-before, and configured audience.
     *
     * @param issuerUri trusted OIDC issuer URI.
     * @param audience required API audience claim.
     * @return configured JWT decoder.
     * @throws IllegalArgumentException when the issuer or audience is blank.
     */
    fun create(issuerUri: String, audience: String): JwtDecoder {
        require(issuerUri.isNotBlank()) { "OIDC issuer URI is required" }
        require(audience.isNotBlank()) { "OIDC audience is required" }

        val decoder = JwtDecoders.fromIssuerLocation(issuerUri) as NimbusJwtDecoder
        val audienceValidator = JwtClaimValidator<Collection<String>>("aud") { values ->
            values?.contains(audience) == true
        }
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                audienceValidator
            )
        )
        return decoder
    }
}
