package com.subhrodip.squarewise.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies normalization and defaulting of the configured JWT algorithm allow-list. */
class OidcSecurityConstantsTest {
    @Test
    fun `normalizes comma separated algorithm configuration`() {
        assertEquals(
            setOf("RS256", "RS512"),
            OidcSecurityConstants.configuredSigningAlgorithms(" RS256, RS512, RS256 ")
        )
    }

    @Test
    fun `defaults blank algorithm configuration to asymmetric default`() {
        assertEquals(setOf("RS256"), OidcSecurityConstants.configuredSigningAlgorithms(" ,  "))
    }
}
