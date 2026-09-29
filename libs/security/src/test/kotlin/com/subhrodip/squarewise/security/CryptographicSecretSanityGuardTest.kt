package com.subhrodip.squarewise.security

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies fail-closed startup validation for cryptographic secrets in production/staging. */
class CryptographicSecretSanityGuardTest {

    @Test
    fun `accepts secure non-blacklisted random secret`() {
        assertDoesNotThrow {
            CryptographicSecretSanityGuard(
                credentialDigestSecret = "c29tZS1yYW5kb20tc2VjdXJlLWtleS0zMi1ieXRlcw==",
                authEmailEnvelopeKey = "YW5vdGhlci1yYW5kb20tc2VjdXJlLWtleS0zMi1ieXRl"
            )
        }
    }

    @Test
    fun `rejects known predictable CI fixture key for credential digest`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CryptographicSecretSanityGuard(
                credentialDigestSecret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
                authEmailEnvelopeKey = "c29tZS1yYW5kb20tc2VjdXJlLWtleS0zMi1ieXRlcw=="
            )
        }
        assert(ex.message!!.contains("credential-digest-secret"))
    }

    @Test
    fun `rejects known predictable CI fixture key for auth email envelope`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CryptographicSecretSanityGuard(
                credentialDigestSecret = "c29tZS1yYW5kb20tc2VjdXJlLWtleS0zMi1ieXRlcw==",
                authEmailEnvelopeKey = "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8="
            )
        }
        assert(ex.message!!.contains("auth-email-envelope-key"))
    }

    @Test
    fun `ignores blank secrets when feature not configured`() {
        assertDoesNotThrow {
            CryptographicSecretSanityGuard(
                credentialDigestSecret = "",
                authEmailEnvelopeKey = ""
            )
        }
    }
}
