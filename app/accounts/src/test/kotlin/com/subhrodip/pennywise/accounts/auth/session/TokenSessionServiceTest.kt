package com.subhrodip.pennywise.accounts.auth.session

import com.subhrodip.pennywise.accounts.auth.credential.HmacCredentialDigest
import com.subhrodip.pennywise.accounts.auth.provider.InternalJwtTokenProvider
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TokenSessionServiceTest @Autowired constructor(
    private val sessionRepository: AuthSessionRepository
) {
    private val secret = ByteArray(32) { it.toByte() }
    private val digest = HmacCredentialDigest(secret)
    private val tokenProvider = InternalJwtTokenProvider(
        secretSigningKey = secret,
        issuerUri = "https://issuer.example.pennywise",
        audience = "pennywise-api"
    )
    private val service = TokenSessionService(
        sessionRepository = sessionRepository,
        identityProviderPort = tokenProvider,
        credentialDigest = digest,
        refreshTokenLifetime = Duration.ofDays(30)
    )

    @Test
    fun `creates initial session with valid tokens`() {
        val now = Instant.now()
        val accountId = UUID.randomUUID()
        val response = service.createSession(
            accountId = accountId,
            subject = "internal:user@example.com",
            email = "user@example.com",
            clientKind = "BROWSER",
            deviceLabel = "Mozilla/5.0",
            now = now
        )

        assertNotNull(response.accessToken)
        assertEquals("Bearer", response.tokenType)
        assertTrue(response.expiresIn > 0)
        assertNotNull(response.refreshToken)

        val stored = sessionRepository.findByRefreshTokenDigest(digest.digest(response.refreshToken))
        assertNotNull(stored)
        assertEquals(accountId, stored?.accountId)
        assertEquals("BROWSER", stored?.clientKind)
    }

    @Test
    fun `rotates refresh token within the same family`() {
        val now = Instant.now()
        val accountId = UUID.randomUUID()
        val initial = service.createSession(
            accountId = accountId,
            subject = "internal:rotate@example.com",
            email = "rotate@example.com",
            clientKind = "BROWSER",
            deviceLabel = "test",
            now = now
        )

        val rotated = service.rotateSession(
            rawRefreshToken = initial.refreshToken,
            clientKind = "BROWSER",
            deviceLabel = "test-2",
            now = now.plusSeconds(10),
            subject = "internal:rotate@example.com",
            email = "rotate@example.com"
        )

        assertNotNull(rotated.accessToken)
        assertNotNull(rotated.refreshToken)
        assertTrue(rotated.refreshToken != initial.refreshToken)

        // Old token session is marked replaced and revoked
        val oldSession = sessionRepository.findByRefreshTokenDigest(digest.digest(initial.refreshToken))
        assertNotNull(oldSession?.revokedAt)
        assertNotNull(oldSession?.replacedBySessionId)

        // New token session belongs to same family
        val newSession = sessionRepository.findByRefreshTokenDigest(digest.digest(rotated.refreshToken))
        assertNotNull(newSession)
        assertEquals(oldSession?.familyId, newSession?.familyId)
    }

    @Test
    fun `detects token reuse and revokes entire family`() {
        val now = Instant.now()
        val accountId = UUID.randomUUID()
        val initial = service.createSession(
            accountId = accountId,
            subject = "internal:reuse@example.com",
            email = "reuse@example.com",
            clientKind = "BROWSER",
            deviceLabel = "test",
            now = now
        )

        // Rotate once (valid)
        val rotated = service.rotateSession(
            rawRefreshToken = initial.refreshToken,
            clientKind = "BROWSER",
            deviceLabel = "test-2",
            now = now.plusSeconds(10),
            subject = "internal:reuse@example.com",
            email = "reuse@example.com"
        )

        // Presenting old token again (reuse attack)
        val ex = assertThrows(ApplicationException::class.java) {
            service.rotateSession(
                rawRefreshToken = initial.refreshToken,
                clientKind = "BROWSER",
                deviceLabel = "attacker",
                now = now.plusSeconds(20),
                subject = "internal:reuse@example.com",
                email = "reuse@example.com"
            )
        }
        assertEquals(ErrorCode.ERR_03, ex.errorCode)

        // The whole family, including rotated, must now be revoked
        val rotatedSession = sessionRepository.findByRefreshTokenDigest(digest.digest(rotated.refreshToken))
        assertNotNull(rotatedSession?.revokedAt)
    }
}
