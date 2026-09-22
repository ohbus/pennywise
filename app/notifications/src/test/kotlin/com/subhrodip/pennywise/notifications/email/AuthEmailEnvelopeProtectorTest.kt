package com.subhrodip.pennywise.notifications.email
import com.subhrodip.pennywise.notifications.email.security.AuthEmailEnvelopeProtector
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AuthEmailEnvelopeProtectorTest {
    private val key = ByteArray(32) { it.toByte() }
    private val protector = AuthEmailEnvelopeProtector(key)

    @Test
    fun `decrypts envelope only for matching context`() {
        val envelope = encrypt("code-123", "person@example.com", "LOGIN_CODE")

        assertEquals("code-123", protector.reveal(envelope, "person@example.com", "LOGIN_CODE"))
        assertThrows(IllegalArgumentException::class.java) {
            protector.reveal(envelope, "other@example.com", "LOGIN_CODE")
        }
    }

    private fun encrypt(value: String, recipient: String, template: String): String {
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD("v1|$recipient|$template".toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1) + nonce + cipher.doFinal(value.toByteArray()))
    }
}
