package com.subhrodip.pennywise.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

/** Verifies the configurable asymmetric JWT algorithm policy. */
class OidcJwtAlgorithmPolicyTest {
    @Test
    fun `accepts configured algorithm`() {
        assertFalse(policy("RS256").validate(jwt("RS256")).hasErrors())
    }

    @Test
    fun `rejects unconfigured algorithm`() {
        assertTrue(policy("RS256").validate(jwt("RS512")).hasErrors())
    }

    @Test
    fun `rejects blank algorithm`() {
        assertTrue(policy("RS256").validate(jwt("")).hasErrors())
    }

    @Test
    fun `rejects symmetric algorithms in configuration`() {
        assertThrows(IllegalArgumentException::class.java) { policy("HS256") }
    }

    private fun policy(algorithm: String) = OidcJwtAlgorithmPolicy(setOf(algorithm))

    private fun jwt(algorithm: String?) = Jwt.withTokenValue("token")
        .apply { if (algorithm != null) header("alg", algorithm) }
        .claim("sub", "subject")
        .build()
}
