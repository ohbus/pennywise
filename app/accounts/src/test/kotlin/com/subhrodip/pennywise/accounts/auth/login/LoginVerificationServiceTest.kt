package com.subhrodip.pennywise.accounts.auth.login

import com.subhrodip.pennywise.accounts.auth.credential.HmacCredentialDigest
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialRepository
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialService
import com.subhrodip.pennywise.accounts.auth.credential.OneTimeCredentialIssuer
import com.subhrodip.pennywise.accounts.auth.provider.InternalJwtTokenProvider
import com.subhrodip.pennywise.accounts.auth.session.AuthSessionRepository
import com.subhrodip.pennywise.accounts.auth.session.TokenSessionService
import com.subhrodip.pennywise.accounts.profile.persistence.InMemoryProfileStore
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class LoginVerificationServiceTest @Autowired constructor(
    private val credentialRepository: LoginCredentialRepository,
    private val sessionRepository: AuthSessionRepository
) {
    private val secret = ByteArray(32) { it.toByte() }
    private val digest = HmacCredentialDigest(secret)
    private val issuer = OneTimeCredentialIssuer(digest)
    private val credentialService = LoginCredentialService(credentialRepository, issuer)
    private val profileStore = InMemoryProfileStore()
    private val tokenProvider = InternalJwtTokenProvider(
        secretSigningKey = secret,
        issuerUri = "https://issuer.example.pennywise",
        audience = "pennywise-api"
    )
    private val tokenSessionService = TokenSessionService(
        sessionRepository = sessionRepository,
        identityProviderPort = tokenProvider,
        credentialDigest = digest
    )
    private val service = LoginVerificationService(
        credentialService = credentialService,
        profileStore = profileStore,
        tokenSessionService = tokenSessionService
    )

    @Test
    fun `redeems issued credential successfully and provisions profile`() {
        val now = Instant.now()
        val issued = credentialService.issue(
            email = "login@example.com",
            kind = LoginCredentialService.CredentialKind.LINK,
            now = now
        )

        val tokens = service.verify(
            credential = issued.plaintext,
            clientKind = "BROWSER",
            deviceLabel = "test",
            now = now.plusSeconds(5)
        )

        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)

        val profile = profileStore.get("internal:login@example.com")
        assertNotNull(profile)
        assertEquals("internal:login@example.com", profile.displayName)
    }

    @Test
    fun `fails closed when credential is invalid or already consumed`() {
        val now = Instant.now()
        val issued = credentialService.issue(
            email = "once@example.com",
            kind = LoginCredentialService.CredentialKind.CODE,
            now = now
        )

        // First redemption succeeds
        service.verify(issued.plaintext, "NATIVE", null, now.plusSeconds(1))

        // Second redemption fails closed
        val ex = assertThrows(ApplicationException::class.java) {
            service.verify(issued.plaintext, "NATIVE", null, now.plusSeconds(2))
        }
        assertEquals(ErrorCode.ERR_03, ex.errorCode)
    }
}
