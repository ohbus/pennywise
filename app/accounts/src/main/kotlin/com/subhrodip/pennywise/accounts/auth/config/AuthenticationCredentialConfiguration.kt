package com.subhrodip.pennywise.accounts.auth.config

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.HmacCredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialRepository
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.credential.OneTimeCredentialIssuer
import com.subhrodip.pennywise.accounts.auth.delivery.AesGcmCredentialEnvelopeProtector
import com.subhrodip.pennywise.accounts.auth.delivery.CredentialEnvelopeProtector
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Fail-closed deployment wiring for passwordless credential cryptography. */
@Configuration
class AuthenticationCredentialConfiguration(
    @Value("\${PENNYWISE_SECURITY_CREDENTIAL_DIGEST_SECRET:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}")
    private val encodedDigestSecret: String,
    @Value("\${PENNYWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}")
    private val encodedEnvelopeKey: String
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

    /** Creates the fail-closed AES-GCM protector for auth-email handoffs. */
    @Bean
    fun credentialEnvelopeProtector(): CredentialEnvelopeProtector =
        AesGcmCredentialEnvelopeProtector(decodeEnvelopeKey())

    /** Creates the rate limit key deriver. */
    @Bean
    fun loginRateLimitKeyDeriver(digest: CredentialDigest): com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitKeyDeriver =
        com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitKeyDeriver(digest)

    /** Creates the transactional login rate limit service. */
    @Bean
    fun loginRateLimitService(
        keyDeriver: com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitKeyDeriver,
        repository: com.subhrodip.pennywise.accounts.auth.abuse.RateLimitBucketRepository
    ): com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitService =
        com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitService(keyDeriver, repository)

    /** Creates the login start application service. */
    @Bean
    fun loginStartService(
        rateLimitService: com.subhrodip.pennywise.accounts.auth.abuse.LoginRateLimitService,
        credentialService: LoginCredentialService,
        emailSender: com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailSender
    ): com.subhrodip.pennywise.accounts.auth.login.LoginStartService =
        com.subhrodip.pennywise.accounts.auth.login.LoginStartService(rateLimitService, credentialService, emailSender)

    private fun decodeSecret(): ByteArray = runCatching {
        Base64.getDecoder().decode(encodedDigestSecret)
    }.getOrElse { throw IllegalArgumentException("Credential digest secret must be base64", it) }
        .also { require(it.size >= 32) { "Credential digest secret must contain at least 32 bytes" } }

    private fun decodeEnvelopeKey(): ByteArray = runCatching {
        Base64.getDecoder().decode(encodedEnvelopeKey)
    }.getOrElse { throw IllegalArgumentException("Auth email envelope key must be base64", it) }
        .also { require(it.size == 32) { "Auth email envelope key must contain exactly 32 bytes" } }
}
