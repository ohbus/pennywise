package com.subhrodip.squarewise.security

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

/** Verifies the provider-neutral durable-subject claim policy. */
class OidcJwtClaimPolicyTest {
    @Test
    fun `accepts bounded non-whitespace subject`() {
        assertValid("provider-subject-123")
    }

    @Test
    fun `rejects missing subject`() {
        assertInvalid(null)
    }

    @Test
    fun `rejects blank or whitespace subject`() {
        assertInvalid("")
        assertInvalid(" ")
        assertInvalid("subject with spaces")
    }

    @Test
    fun `rejects subject over maximum length`() {
        assertInvalid("x".repeat(OidcJwtClaimPolicy.MAX_SUBJECT_LENGTH + 1))
    }

    /** Non-string subject claims are rejected instead of being coerced into identities. */
    @Test
    fun `rejects non string subject`() {
        val token = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("sub", 123)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build()

        assertTrue(OidcJwtClaimPolicy.subjectValidator().validate(token).hasErrors())
    }

    private fun assertValid(subject: String) {
        assertFalse(validate(subject).hasErrors())
    }

    private fun assertInvalid(subject: String?) {
        assertTrue(validate(subject).hasErrors())
    }

    private fun validate(subject: String?) = OidcJwtClaimPolicy.subjectValidator().validate(
        Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .apply { if (subject != null) claim("sub", subject) }
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build()
    )
}
