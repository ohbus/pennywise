package com.subhrodip.squarewise.notifications.security

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies Notifications constructs its provider-backed JWT decoder from configuration. */
class ProductionSecurityConfigTest {
    /** Verifies invalid issuer configuration fails closed before any remote discovery. */
    @Test
    fun `rejects blank issuer in configured oidc policy`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductionSecurityConfig("", "squarewise-api", "RS256").jwtDecoder()
        }
    }
}
