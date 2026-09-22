package com.subhrodip.pennywise.accounts.auth.config

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.login.LoginVerificationService
import com.subhrodip.pennywise.accounts.auth.provider.ExternalOidcTokenProvider
import com.subhrodip.pennywise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.pennywise.accounts.auth.session.AuthSessionRepository
import com.subhrodip.pennywise.accounts.auth.session.TokenSessionService
import com.subhrodip.pennywise.accounts.profile.persistence.ProfileStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Spring configuration wiring token minting, session management, and login verification.
 */
@Configuration
class AuthSessionConfiguration(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @Value("\${pennywise.security.oidc.audience}")
    private val audience: String
) {
    /**
     * Registers the configured external OIDC adapter for provider-backed profiles.
     *
     * The adapter owns delegation to the external issuer; this configuration must
     * not fall back to the internal HMAC signer when OIDC security is enabled.
     */
    @Bean
    @Profile("production", "staging", "local-oidc")
    fun externalIdentityProviderPort(): IdentityProviderPort = ExternalOidcTokenProvider(
        externalIssuerUri = issuerUri,
        clientId = audience,
        audience = audience
    )

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

}
