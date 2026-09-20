package com.subhrodip.pennywise.accounts.auth.delivery

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows

class CredentialEnvelopeProtectorTest {
    private val protector = AesGcmCredentialEnvelopeProtector(ByteArray(32) { it.toByte() })
    private val context = CredentialDeliveryContext("person@example.com", AuthEmailTemplate.LOGIN_LINK)

    @Test
    fun `protect and reveal round trip`() {
        val envelope = protector.protect("one-time-secret", context)

        assertEquals("one-time-secret", protector.reveal(envelope, context))
        assertNotEquals("one-time-secret", envelope)
    }

    @Test
    fun `fresh nonce produces different envelopes`() {
        assertNotEquals(
            protector.protect("same-secret", context),
            protector.protect("same-secret", context)
        )
    }

    @Test
    fun `context mismatch is rejected`() {
        val envelope = protector.protect("one-time-secret", context)

        assertThrows(IllegalArgumentException::class.java) {
            protector.reveal(
                envelope,
                CredentialDeliveryContext("other@example.com", AuthEmailTemplate.LOGIN_LINK)
            )
        }
    }

    @Test
    fun `tampering is rejected`() {
        val envelope = protector.protect("one-time-secret", context)
        val tampered = envelope.dropLast(1) + if (envelope.last() == 'A') 'B' else 'A'

        assertThrows(IllegalArgumentException::class.java) { protector.reveal(tampered, context) }
    }

    @Test
    fun `invalid key size fails closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCredentialEnvelopeProtector(ByteArray(16))
        }
    }
}
