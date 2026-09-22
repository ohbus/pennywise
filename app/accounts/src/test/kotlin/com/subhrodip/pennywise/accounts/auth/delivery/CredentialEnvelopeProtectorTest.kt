package com.subhrodip.pennywise.accounts.auth.delivery

import com.subhrodip.pennywise.accounts.auth.delivery.model.AuthEmailTemplate
import com.subhrodip.pennywise.accounts.auth.delivery.model.CredentialDeliveryContext
import com.subhrodip.pennywise.accounts.auth.delivery.security.AesGcmCredentialEnvelopeProtector

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import java.util.Base64

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
        val decoded = Base64.getUrlDecoder().decode(envelope)
        decoded[decoded.lastIndex] = (decoded[decoded.lastIndex].toInt() xor 1).toByte()
        val tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded)

        assertThrows(IllegalArgumentException::class.java) { protector.reveal(tampered, context) }
    }

    @Test
    fun `invalid key size fails closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCredentialEnvelopeProtector(ByteArray(16))
        }
    }
}
