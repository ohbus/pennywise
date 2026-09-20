package com.subhrodip.pennywise.accounts.auth.config

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.HmacCredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialRepository
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.credential.OneTimeCredentialIssuer
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Fail-closed deployment wiring for passwordless credential cryptography. */
@Configuration
@Profile("local-oidc", "staging", "production")
class AuthenticationCredentialConfiguration(
    @Value("\${PENNYWISE_SECURITY_CREDENTIAL_DIGEST_SECRET}")
    private val encodedDigestSecret: String
) {
    /** Creates the HMAC digest adapter from a deployment-only base64 secret. */
    @Bean
    fun credentialDigest(): CredentialDigest = HmacCredentialDigest(decodeSecret())

    /** Creates the secure one-time credential issuer. */
    @Bean
    fun oneTimeCredentialIssuer(digest: CredentialDigest): OneTimeCredentialIssuer =
        OneTimeCredentialIssuer(digest)

    /** Creates the transactional application service for login credentials. */
    @Bean
    fun loginCredentialService(
        repository: LoginCredentialRepository,
        issuer: OneTimeCredentialIssuer
    ): LoginCredentialService = LoginCredentialService(repository, issuer)

    private fun decodeSecret(): ByteArray = runCatching {
        Base64.getDecoder().decode(encodedDigestSecret)
    }.getOrElse { throw IllegalArgumentException("Credential digest secret must be base64", it) }
        .also { require(it.size >= 32) { "Credential digest secret must contain at least 32 bytes" } }
}
