package com.subhrodip.pennywise.notifications.email

import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Fail-closed configuration for decrypting protected auth-email handoffs. */
@Configuration
class AuthEmailSecurityConfiguration(
    @Value("\${PENNYWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY}") private val encodedKey: String
) {
    /** Creates the configured AES-GCM decryptor; missing or malformed keys fail startup. */
    @Bean
    fun authEmailEnvelopeProtector(): AuthEmailEnvelopeProtector = AuthEmailEnvelopeProtector(
        runCatching { Base64.getDecoder().decode(encodedKey) }
            .getOrElse { throw IllegalArgumentException("Auth email envelope key must be base64", it) }
            .also { require(it.size == 32) { "Auth email envelope key must contain exactly 32 bytes" } }
    )
}
