package com.subhrodip.squarewise.notifications.email

import com.subhrodip.squarewise.notifications.email.security.AuthEmailSecurityConfiguration
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies auth-email envelope key configuration fails closed before delivery starts. */
class AuthEmailSecurityConfigurationTest {
    @Test
    fun `creates protector from an exact 32 byte base64 key`() {
        val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

        assertNotNull(AuthEmailSecurityConfiguration(key).authEmailEnvelopeProtector())
    }

    @Test
    fun `rejects malformed and incorrectly sized keys`() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthEmailSecurityConfiguration("not-base64!").authEmailEnvelopeProtector()
        }
        assertThrows(IllegalArgumentException::class.java) {
            AuthEmailSecurityConfiguration(Base64.getEncoder().encodeToString(ByteArray(31))).authEmailEnvelopeProtector()
        }
    }
}
