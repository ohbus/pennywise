package com.subhrodip.pennywise.security

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies the production-like OIDC configuration fail-closed boundary. */
class OidcConfigurationGuardTest {
    @Test
    fun `accepts secure issuer and audience`() {
        assertDoesNotThrow {
            OidcConfigurationGuard("https://keycloak.example/realms/pennywise", "pennywise-api", "production")
        }
    }

    @Test
    fun `rejects missing issuer`() {
        assertThrows(IllegalArgumentException::class.java) {
            OidcConfigurationGuard("", "pennywise-api", "production")
        }
    }

    @Test
    fun `rejects missing audience`() {
        assertThrows(IllegalArgumentException::class.java) {
            OidcConfigurationGuard("https://keycloak.example/realms/pennywise", "", "production")
        }
    }

    @Test
    fun `rejects non-https issuer`() {
        assertThrows(IllegalArgumentException::class.java) {
            OidcConfigurationGuard("http://keycloak.example/realms/pennywise", "pennywise-api", "production")
        }
    }
}
