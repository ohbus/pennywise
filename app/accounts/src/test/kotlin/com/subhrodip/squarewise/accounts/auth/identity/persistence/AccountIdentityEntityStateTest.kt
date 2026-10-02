package com.subhrodip.squarewise.accounts.auth.identity.persistence

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies durable provider identity state across enrollment and suspension. */
class AccountIdentityEntityStateTest {
    @Test
    fun `identity retains provider binding and mutable lifecycle metadata`() {
        val identityId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val enrolledAt = Instant.parse("2026-10-02T12:00:00Z")
        val identity = AccountIdentityEntity(
            identityId = identityId,
            accountId = accountId,
            issuer = "https://issuer.example",
            providerSubject = "provider-subject-1",
            email = "alice@example.com",
            emailVerified = false,
            createdAt = enrolledAt,
            updatedAt = enrolledAt
        )

        val updatedIdentityId = UUID.randomUUID()
        identity.identityId = updatedIdentityId
        identity.accountId = accountId
        identity.issuer = "https://issuer.example/v2"
        identity.providerSubject = "provider-subject-2"
        identity.email = "alice+verified@example.com"
        identity.emailVerified = true
        identity.status = "SUSPENDED"
        identity.createdAt = enrolledAt.minusSeconds(60)
        identity.updatedAt = enrolledAt

        assertEquals(updatedIdentityId, identity.identityId)
        assertEquals(accountId, identity.accountId)
        assertEquals("https://issuer.example/v2", identity.issuer)
        assertEquals("provider-subject-2", identity.providerSubject)
        assertEquals("alice+verified@example.com", identity.email)
        assertEquals(true, identity.emailVerified)
        assertEquals("SUSPENDED", identity.status)
        assertEquals(enrolledAt.minusSeconds(60), identity.createdAt)
        assertEquals(enrolledAt, identity.updatedAt)
    }
}
