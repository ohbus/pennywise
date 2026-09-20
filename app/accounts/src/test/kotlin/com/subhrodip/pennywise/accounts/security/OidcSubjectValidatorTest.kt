package com.subhrodip.pennywise.accounts.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URI

class OidcSubjectValidatorTest {
    private val validator = OidcSubjectValidator(URI("https://issuer.example"))
    @Test
    fun `accepts trusted issuer and bounded subject`() = assertEquals("auth0|alice", validator.validate(OidcClaims("https://issuer.example", "auth0|alice")))
    @Test
    fun `rejects wrong issuer and malformed subject`() {
        assertThrows(IllegalArgumentException::class.java) { validator.validate(OidcClaims("https://evil.example", "alice")) }
        assertThrows(IllegalArgumentException::class.java) { validator.validate(OidcClaims("https://issuer.example", "alice space")) }
    }
}
