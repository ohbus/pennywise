package com.subhrodip.pennywise.security

import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator

/** Shared provider-neutral claim rules applied after JWT signature validation. */
object OidcJwtClaimPolicy {
    /** Maximum subject length accepted as a durable provider identity. */
    const val MAX_SUBJECT_LENGTH: Int = 256

    private val subjectPattern = Regex("^[^\\s]{1,$MAX_SUBJECT_LENGTH}$")

    /**
     * Creates the subject validator used by servlet and reactive decoders.
     *
     * A subject must be present, non-blank, contain no whitespace, and remain
     * bounded so malformed provider claims cannot become unbounded identity
     * keys. Email and display claims are intentionally not considered.
     *
     * @return validator that fails closed for absent or malformed subjects.
     */
    fun subjectValidator(): OAuth2TokenValidator<Jwt> = JwtClaimValidator<Any>(OidcSecurityConstants.SUBJECT_CLAIM) { subject ->
        subject is String && subjectPattern.matches(subject)
    }
}
