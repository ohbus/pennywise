package com.subhrodip.pennywise.security

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators

/** Verifies provider-neutral expiry and not-before validation semantics. */
class OidcJwtTemporalPolicyTest {
    private val issuer = "https://issuer.example.test"
    private val now = Instant.parse("2100-01-01T12:00:00Z")

    @Test
    fun `rejects token whose expiry is in the past`() {
        val token = jwt(
            expiresAt = Instant.parse("2000-01-01T00:00:00Z"),
            issuedAt = Instant.parse("1999-12-31T23:59:00Z")
        )

        assertTrue(defaultValidators().validate(token).hasErrors())
    }

    @Test
    fun `rejects token that is not valid yet`() {
        val token = jwt(
            expiresAt = now.plusSeconds(300),
            issuedAt = now,
            notBefore = now.plusSeconds(30)
        )

        assertTrue(defaultValidators().validate(token).hasErrors())
    }

    @Test
    fun `accepts token inside its validity window`() {
        val token = jwt(expiresAt = now.plusSeconds(300), issuedAt = now.minusSeconds(30))

        assertFalse(defaultValidators().validate(token).hasErrors())
    }

    private fun defaultValidators() = JwtValidators.createDefaultWithIssuer(issuer)

    private fun jwt(
        expiresAt: Instant,
        issuedAt: Instant,
        notBefore: Instant? = null
    ): Jwt = Jwt.withTokenValue("token")
        .header("alg", OidcSecurityConstants.DEFAULT_SIGNING_ALGORITHM)
        .issuer(issuer)
        .subject("provider-subject-123")
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .apply { if (notBefore != null) notBefore(notBefore) }
        .build()
}
