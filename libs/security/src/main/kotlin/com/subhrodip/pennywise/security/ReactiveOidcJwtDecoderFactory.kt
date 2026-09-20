package com.subhrodip.pennywise.security

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.JwtClaimValidator

/** Builds the reactive equivalent of the shared provider-neutral JWT policy. */
object ReactiveOidcJwtDecoderFactory {
    /** Creates an issuer-discovered reactive decoder with issuer and audience validation. */
    fun create(
        issuerUri: String,
        audience: String,
        allowedAlgorithms: Set<String> = setOf(OidcSecurityConstants.DEFAULT_SIGNING_ALGORITHM)
    ): ReactiveJwtDecoder {
        require(issuerUri.isNotBlank()) { OidcSecurityConstants.ISSUER_REQUIRED_MESSAGE }
        require(audience.isNotBlank()) { OidcSecurityConstants.AUDIENCE_REQUIRED_MESSAGE }
        val decoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                JwtClaimValidator<Collection<String>>(OidcSecurityConstants.AUDIENCE_CLAIM) { values -> values.contains(audience) },
                OidcJwtClaimPolicy.subjectValidator(),
                OidcJwtAlgorithmPolicy(allowedAlgorithms)
            )
        )
        return decoder
    }
}
