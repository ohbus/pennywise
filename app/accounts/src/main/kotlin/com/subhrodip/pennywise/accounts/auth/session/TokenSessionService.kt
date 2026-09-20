package com.subhrodip.pennywise.accounts.auth.session

import com.subhrodip.pennywise.accounts.auth.credential.CredentialDigest
import com.subhrodip.pennywise.accounts.auth.provider.IdentityProviderPort
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * Coordinates token issuance, refresh token rotation, and family-wide revocation.
 *
 * Implements strict OAuth 2.0 refresh token family tracking and reuse detection:
 * - Each verify login creates a new refresh token family.
 * - Refresh operations atomically consume the old token and issue a child session in the same family.
 * - If an already-rotated (or revoked) refresh token is presented, the entire family is immediately revoked
 *   and an unauthorized exception is raised (reuse detection).
 *
 * @param sessionRepository Auth session persistence port.
 * @param identityProviderPort Provider-neutral access token minting port.
 * @param credentialDigest HMAC digest generator for hashing refresh tokens at rest.
 * @param random Cryptographically secure random source for opaque refresh tokens.
 * @param refreshTokenLifetime Validity duration for refresh tokens (defaults to 30 days).
 */
open class TokenSessionService(
    private val sessionRepository: AuthSessionRepository,
    private val identityProviderPort: IdentityProviderPort,
    private val credentialDigest: CredentialDigest,
    private val random: SecureRandom = SecureRandom(),
    private val refreshTokenLifetime: Duration = DEFAULT_REFRESH_LIFETIME
) {
    private val log = LoggerFactory.getLogger(TokenSessionService::class.java)

    /**
     * Creates an initial session family and issues both access and refresh tokens.
     *
     * @param accountId Stable account identifier UUID.
     * @param subject Canonical identity subject string.
     * @param email Canonical normalized user email.
     * @param clientKind Client type ("BROWSER" or "NATIVE").
     * @param deviceLabel Optional user-agent or client label.
     * @param now Current timestamp.
     * @return [TokenResponse] containing signed access token and new opaque refresh token.
     */
    @Transactional
    fun createSession(
        accountId: UUID,
        subject: String,
        email: String,
        clientKind: String,
        deviceLabel: String?,
        now: Instant
    ): TokenResponse {
        val familyId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val rawRefreshToken = generateOpaqueToken()
        val digest = credentialDigest.digest(rawRefreshToken)

        val session = AuthSessionEntity(
            sessionId = sessionId,
            accountId = accountId,
            familyId = familyId,
            refreshTokenDigest = digest,
            createdAt = now,
            lastUsedAt = now,
            expiresAt = now.plus(refreshTokenLifetime),
            revokedAt = null,
            replacedBySessionId = null,
            deviceLabel = deviceLabel?.take(MAX_DEVICE_LABEL_LENGTH),
            clientKind = clientKind
        )
        sessionRepository.save(session)

        val issuedToken = identityProviderPort.issueAccessToken(accountId, subject, email)
        return TokenResponse(
            accessToken = issuedToken.accessToken,
            tokenType = issuedToken.tokenType,
            expiresIn = issuedToken.expiresIn,
            refreshToken = rawRefreshToken
        )
    }

    /**
     * Rotates a refresh token atomically within its family or detects token reuse.
     *
     * @param rawRefreshToken Presented opaque refresh token.
     * @param clientKind Client type ("BROWSER" or "NATIVE").
     * @param deviceLabel Optional user-agent or client label.
     * @param now Current timestamp.
     * @param subject Canonical identity subject for minting the new access token.
     * @param email Canonical user email.
     * @return [TokenResponse] with fresh access token and child refresh token.
     * @throws ApplicationException with [ErrorCode.ERR_03] when invalid, expired, revoked, or reused.
     */
    @Transactional
    fun rotateSession(
        rawRefreshToken: String,
        clientKind: String,
        deviceLabel: String?,
        now: Instant,
        subject: String,
        email: String
    ): TokenResponse {
        if (rawRefreshToken.isBlank()) {
            throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
        }

        val digest = credentialDigest.digest(rawRefreshToken)
        val existingSession = sessionRepository.findByRefreshTokenDigest(digest)
            ?: throw ApplicationException(ErrorCode.ERR_03, "Authentication required")

        // Reuse detection: if this session was already replaced or revoked, revoke entire family
        if (existingSession.revokedAt != null || existingSession.replacedBySessionId != null) {
            log.warn("Refresh token reuse detected for session family {}. Revoking family.", existingSession.familyId)
            sessionRepository.revokeFamily(existingSession.familyId, now)
            throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
        }

        // Expiry check
        if (existingSession.expiresAt.isBefore(now)) {
            sessionRepository.revokeFamily(existingSession.familyId, now)
            throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
        }

        val newSessionId = UUID.randomUUID()
        val newRawRefreshToken = generateOpaqueToken()
        val newDigest = credentialDigest.digest(newRawRefreshToken)

        val accountId = existingSession.accountId
            ?: throw ApplicationException(ErrorCode.ERR_03, "Authentication required")

        val newSession = AuthSessionEntity(
            sessionId = newSessionId,
            accountId = accountId,
            familyId = existingSession.familyId,
            refreshTokenDigest = newDigest,
            createdAt = now,
            lastUsedAt = now,
            expiresAt = now.plus(refreshTokenLifetime),
            revokedAt = null,
            replacedBySessionId = null,
            deviceLabel = deviceLabel?.take(MAX_DEVICE_LABEL_LENGTH) ?: existingSession.deviceLabel,
            clientKind = clientKind
        )
        sessionRepository.save(newSession)

        val updatedCount = sessionRepository.rotateIfActive(existingSession.sessionId, newSessionId, now)
        if (updatedCount != 1) {
            // Concurrent race or collision: fail closed and revoke family
            sessionRepository.revokeFamily(existingSession.familyId, now)
            throw ApplicationException(ErrorCode.ERR_03, "Authentication required")
        }

        val issuedToken = identityProviderPort.issueAccessToken(accountId, subject, email)
        return TokenResponse(
            accessToken = issuedToken.accessToken,
            tokenType = issuedToken.tokenType,
            expiresIn = issuedToken.expiresIn,
            refreshToken = newRawRefreshToken
        )
    }

    /**
     * Revokes all active sessions in the family associated with the given refresh token.
     *
     * @param rawRefreshToken Refresh token to revoke.
     * @param now Revocation timestamp.
     */
    @Transactional
    fun revokeSessionByRefreshToken(rawRefreshToken: String, now: Instant) {
        if (rawRefreshToken.isBlank()) return
        val digest = credentialDigest.digest(rawRefreshToken)
        val session = sessionRepository.findByRefreshTokenDigest(digest) ?: return
        sessionRepository.revokeFamily(session.familyId, now)
    }

    /**
     * Revokes all active sessions in a family directly by family ID.
     *
     * @param familyId Unique family UUID.
     * @param now Revocation timestamp.
     */
    @Transactional
    fun revokeFamily(familyId: UUID, now: Instant) {
        sessionRepository.revokeFamily(familyId, now)
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(REFRESH_TOKEN_BYTE_LENGTH).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val REFRESH_TOKEN_BYTE_LENGTH = 48
        const val MAX_DEVICE_LABEL_LENGTH = 120
        val DEFAULT_REFRESH_LIFETIME: Duration = Duration.ofDays(30)
    }
}
