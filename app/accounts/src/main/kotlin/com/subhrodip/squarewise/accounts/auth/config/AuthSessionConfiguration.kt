@file:Suppress("CanConvertToMultiDollarString")

package com.subhrodip.squarewise.accounts.auth.config

import com.subhrodip.squarewise.accounts.auth.credential.CredentialDigest
import com.subhrodip.squarewise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.squarewise.accounts.auth.login.LoginVerificationService
import com.subhrodip.squarewise.accounts.auth.provider.ExternalOidcTokenProvider
import com.subhrodip.squarewise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.squarewise.accounts.auth.session.AuthSessionRepository
import com.subhrodip.squarewise.accounts.auth.session.SessionPolicy
import com.subhrodip.squarewise.accounts.auth.session.SessionPolicyProperties
import com.subhrodip.squarewise.accounts.auth.session.TokenSessionService
import com.subhrodip.squarewise.accounts.profile.persistence.ProfileStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Spring configuration wiring token minting, session management, and login verification.
 */
@Configuration
@EnableConfigurationProperties(SessionPolicyProperties::class)
class AuthSessionConfiguration(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @Value("\${squarewise.security.oidc.audience}")
    private val audience: String
) {
    /** Creates the single validated timing policy used by Accounts sessions. */
    @Bean
    fun sessionPolicy(properties: SessionPolicyProperties): SessionPolicy = SessionPolicy(
        accessTokenLifetime = properties.accessTokenLifetime,
        refreshIdleLifetime = properties.refreshIdleLifetime,
        absoluteSessionLifetime = properties.absoluteSessionLifetime,
        clockSkew = properties.clockSkew
    )
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
        credentialDigest: CredentialDigest,
        sessionPolicy: SessionPolicy
    ): TokenSessionService =
        TokenSessionService(
            sessionRepository = sessionRepository,
            identityProviderPort = identityProviderPort,
            credentialDigest = credentialDigest,
            sessionPolicy = sessionPolicy
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
