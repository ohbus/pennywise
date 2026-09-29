package com.subhrodip.squarewise.accounts.auth.login

import com.subhrodip.squarewise.accounts.auth.credential.LoginCredentialRepository
import com.subhrodip.squarewise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.squarewise.accounts.auth.jwks.RsaKeyProvider
import com.subhrodip.squarewise.accounts.profile.persistence.ProfileStore
import com.subhrodip.squarewise.security.OidcJwtDecoderFactory
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * Integration test proving closure of finding SEC-001 under the deployed local-oidc profile.
 *
 * Verifies that passwordless verification issues a real, usable RS256 access token
 * rather than throwing UnsupportedOperationException, and that the issued token
 * validates successfully against the configured resource-server JWT decoder.
 */
@SpringBootTest(
    properties = [
        "squarewise.auth-email-outbox.enabled=true",
        "SQUAREWISE_SECURITY_AUTH_EMAIL_ENVELOPE_KEY=ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.squarewise.local",
        "squarewise.security.oidc.audience=squarewise-api"
    ]
)
@ActiveProfiles("test", "local-oidc")
@Transactional
class DeployedPasswordlessTokenIntegrationTest @Autowired constructor(
    private val loginVerificationService: LoginVerificationService,
    private val credentialService: LoginCredentialService,
    private val profileStore: ProfileStore,
    private val rsaKeyProvider: RsaKeyProvider,
    private val jwtDecoder: JwtDecoder,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @Value("\${squarewise.security.oidc.audience}") private val audience: String
) {

    @Test
    fun `deployed passwordless login issues valid RS256 token without throwing`() {
        val now = Instant.now()
        val email = "deployed.user@example.com"
        val credential = credentialService.issue(
            email = email,
            kind = LoginCredentialService.CredentialKind.CODE,
            now = now
        )

        // Verifying passwordless login must succeed and return tokens (closing SEC-001)
        val tokenResponse = loginVerificationService.verify(
            credential = credential.plaintext,
            clientKind = "BROWSER",
            deviceLabel = "Integration-Test-Agent",
            now = now
        )

        assertNotNull(tokenResponse.accessToken)
        assertNotNull(tokenResponse.refreshToken)
        assertEquals("Bearer", tokenResponse.tokenType)

        // Verify that the token is decodable by the Spring Security JwtDecoder
        val decoded = jwtDecoder.decode(tokenResponse.accessToken)
        assertNotNull(decoded)
        assertEquals(issuerUri, decoded.issuer.toString())
        assertEquals(listOf(audience), decoded.audience)
        assertEquals(email, decoded.getClaimAsString("email"))
    }

    @Test
    fun `tampered RS256 token fails closed with BadJwtException`() {
        val now = Instant.now()
        val credential = credentialService.issue(
            email = "tamper.test@example.com",
            kind = LoginCredentialService.CredentialKind.CODE,
            now = now
        )

        val tokenResponse = loginVerificationService.verify(
            credential = credential.plaintext,
            clientKind = "BROWSER",
            deviceLabel = "Test-Device",
            now = now
        )

        val parts = tokenResponse.accessToken.split(".")
        assertEquals(3, parts.size)

        // Tamper signature
        val tamperedSig = if (parts[2].startsWith("A")) "B" + parts[2].substring(1) else "A" + parts[2].substring(1)
        val tamperedToken = "${parts[0]}.${parts[1]}.$tamperedSig"

        assertThrows(BadJwtException::class.java) {
            jwtDecoder.decode(tamperedToken)
        }
    }
}
