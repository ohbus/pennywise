package com.subhrodip.squarewise.accounts.auth.credential

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/** Verifies the persisted one-time credential state remains fully mutable for JPA lifecycle use. */
class LoginCredentialEntityStateTest {

    @Test
    fun `credential state exposes issuance and consumption fields`() {
        val credentialId = UUID.randomUUID()
        val issuedAt = Instant.parse("2026-10-01T09:00:00Z")
        val expiresAt = Instant.parse("2026-10-01T09:05:00Z")
        val consumedAt = Instant.parse("2026-10-01T09:01:00Z")
        val digest = byteArrayOf(1, 2, 3)
        val credential = LoginCredentialEntity(
            credentialId = credentialId,
            canonicalEmail = "person@example.com",
            credentialDigest = digest,
            credentialKind = "CODE",
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            remainingAttempts = 3
        )

        credential.consumedAt = consumedAt
        credential.remainingAttempts = 2

        assertEquals(credentialId, credential.credentialId)
        assertEquals("person@example.com", credential.canonicalEmail)
        assertArrayEquals(digest, credential.credentialDigest)
        assertEquals("CODE", credential.credentialKind)
        assertEquals(issuedAt, credential.issuedAt)
        assertEquals(expiresAt, credential.expiresAt)
        assertEquals(2, credential.remainingAttempts)
        assertEquals(consumedAt, credential.consumedAt)
    }
}
