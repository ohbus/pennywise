package com.subhrodip.pennywise.accounts.auth.config

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.login.LoginVerificationService
import com.subhrodip.pennywise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.pennywise.accounts.auth.provider.InternalJwtTokenProvider
import com.subhrodip.pennywise.accounts.auth.session.AuthSessionRepository
import com.subhrodip.pennywise.accounts.auth.session.TokenSessionService
import com.subhrodip.pennywise.accounts.profile.ProfileStore
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Spring configuration wiring token minting, session management, and login verification.
 */
@Configuration
class AuthSessionConfiguration(
    @Value("\${PENNYWISE_SECURITY_JWT_SIGNING_SECRET:\${PENNYWISE_SECURITY_CREDENTIAL_DIGEST_SECRET:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}}")
    private val encodedJwtSecret: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://issuer.example.pennywise}")
    private val issuerUri: String,
    @Value("\${pennywise.security.oidc.audience:pennywise-api}")
    private val audience: String
) {
    /**
     * Default IdentityProviderPort using Nimbus HMAC-SHA256 signer.
     */
    @Bean
    @ConditionalOnMissingBean(IdentityProviderPort::class)
    @Profile("!production & !staging & !local-oidc")
    fun identityProviderPort(): IdentityProviderPort {
        val effectiveIssuer = issuerUri.ifBlank { "https://issuer.example.pennywise" }
        val effectiveAudience = audience.ifBlank { "pennywise-api" }
        return InternalJwtTokenProvider(
            secretSigningKey = decodeSecret(),
            issuerUri = effectiveIssuer,
            audience = effectiveAudience
        )
    }

    /**
     * Creates the TokenSessionService managing refresh tokens and families.
     */
    @Bean
    fun tokenSessionService(
        sessionRepository: AuthSessionRepository,
        identityProviderPort: IdentityProviderPort,
        credentialDigest: CredentialDigest
    ): TokenSessionService =
        TokenSessionService(
            sessionRepository = sessionRepository,
            identityProviderPort = identityProviderPort,
            credentialDigest = credentialDigest
        )

    /**
     * Creates the LoginVerificationService coordinating redemption and session creation.
     */
    @Bean
    fun loginVerificationService(
        credentialService: LoginCredentialService,
        profileStore: ProfileStore,
        tokenSessionService: TokenSessionService
    ): LoginVerificationService =
        LoginVerificationService(
            credentialService = credentialService,
            profileStore = profileStore,
            tokenSessionService = tokenSessionService
        )

    private fun decodeSecret(): ByteArray = runCatching {
        Base64.getDecoder().decode(encodedJwtSecret)
    }.getOrElse { throw IllegalArgumentException("JWT signing secret must be base64", it) }
        .also { require(it.size >= 32) { "JWT signing secret must contain at least 32 bytes" } }
}
