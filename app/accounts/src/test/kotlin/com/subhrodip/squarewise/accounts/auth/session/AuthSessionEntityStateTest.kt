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

        assertEquals(sessionId, session.sessionId)
        assertEquals(accountId, session.accountId)
        assertEquals("subject-1", session.subject)
        assertEquals(familyId, session.familyId)
        assertArrayEquals(digest, session.refreshTokenDigest)
        assertEquals(createdAt, session.createdAt)
        assertEquals(revokedAt, session.lastUsedAt)
        assertEquals(expiresAt, session.expiresAt)
        assertEquals(absoluteExpiresAt, session.absoluteExpiresAt)
        assertEquals(revokedAt, session.revokedAt)
        assertEquals(replacementId, session.replacedBySessionId)
        assertEquals("laptop", session.deviceLabel)
        assertEquals("WEB", session.clientKind)
    }
}
