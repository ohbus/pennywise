package com.subhrodip.squarewise.accounts.auth.session

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies persisted refresh-session state across its active and revoked lifecycle. */
class AuthSessionEntityStateTest {
    @Test
    fun `session retains identity expiry and replacement state`() {
        val sessionId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val familyId = UUID.randomUUID()
        val replacementId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-10-02T12:00:00Z")
        val expiresAt = createdAt.plusSeconds(900)
        val absoluteExpiresAt = createdAt.plusSeconds(86_400)
        val digest = byteArrayOf(1, 2, 3)
        val session = AuthSessionEntity(
            sessionId = sessionId,
            accountId = accountId,
            subject = "subject-1",
            familyId = familyId,
            refreshTokenDigest = digest,
            createdAt = createdAt,
            lastUsedAt = createdAt,
            expiresAt = expiresAt,
            absoluteExpiresAt = absoluteExpiresAt,
            deviceLabel = "laptop",
            clientKind = "WEB"
        )

        val revokedAt = createdAt.plusSeconds(60)
        session.lastUsedAt = revokedAt
        session.revokedAt = revokedAt
        session.replacedBySessionId = replacementId
        val updatedSessionId = UUID.randomUUID()
        val updatedAccountId = UUID.randomUUID()
        val updatedFamilyId = UUID.randomUUID()
        session.sessionId = updatedSessionId
        session.accountId = updatedAccountId
        session.subject = "subject-2"
        session.familyId = updatedFamilyId
        session.refreshTokenDigest = byteArrayOf(4, 5, 6)
        session.createdAt = createdAt.minusSeconds(30)
        session.expiresAt = expiresAt.plusSeconds(30)
        session.absoluteExpiresAt = absoluteExpiresAt.plusSeconds(30)
        session.deviceLabel = "phone"
        session.clientKind = "NATIVE"

        assertEquals(updatedSessionId, session.sessionId)
        assertEquals(updatedAccountId, session.accountId)
        assertEquals("subject-2", session.subject)
        assertEquals(updatedFamilyId, session.familyId)
        assertArrayEquals(byteArrayOf(4, 5, 6), session.refreshTokenDigest)
        assertEquals(createdAt.minusSeconds(30), session.createdAt)
        assertEquals(revokedAt, session.lastUsedAt)
        assertEquals(expiresAt.plusSeconds(30), session.expiresAt)
        assertEquals(absoluteExpiresAt.plusSeconds(30), session.absoluteExpiresAt)
        assertEquals(revokedAt, session.revokedAt)
        assertEquals(replacementId, session.replacedBySessionId)
        assertEquals("phone", session.deviceLabel)
        assertEquals("NATIVE", session.clientKind)
    }
}
