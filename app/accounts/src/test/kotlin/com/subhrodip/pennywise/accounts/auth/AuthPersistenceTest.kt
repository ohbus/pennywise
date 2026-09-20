package com.subhrodip.pennywise.accounts.auth

import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialEntity
import com.subhrodip.pennywise.accounts.auth.credential.LoginCredentialRepository
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailOutboxEntity
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailOutboxRepository
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailOutboxService
import com.subhrodip.pennywise.accounts.auth.delivery.AuthEmailTemplate
import com.subhrodip.pennywise.accounts.auth.session.AuthSessionEntity
import com.subhrodip.pennywise.accounts.auth.session.AuthSessionRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/** Verifies database-backed one-time redemption and refresh-family transitions. */
@SpringBootTest
@Transactional
class AuthPersistenceTest @Autowired constructor(
    private val credentialRepository: LoginCredentialRepository,
    private val sessionRepository: AuthSessionRepository,
    private val authEmailOutboxRepository: AuthEmailOutboxRepository,
    private val authEmailOutboxService: AuthEmailOutboxService
) {
    @Test
    fun `only one caller can consume an active credential`() {
        val now = Instant.now()
        val digest = byteArrayOf(1, 2, 3, 4)
        credentialRepository.save(LoginCredentialEntity(
            credentialId = UUID.randomUUID(), canonicalEmail = "user@example.com",
            credentialDigest = digest, credentialKind = "CODE", issuedAt = now,
            expiresAt = now.plusSeconds(300), remainingAttempts = 3
        ))

        assertEquals(1, credentialRepository.consumeIfActive(digest, now.plusSeconds(1)))
        assertEquals(0, credentialRepository.consumeIfActive(digest, now.plusSeconds(2)))
    }

    @Test
    fun `expired credential cannot be consumed`() {
        val now = Instant.now()
        val digest = byteArrayOf(5, 6, 7, 8)
        credentialRepository.save(LoginCredentialEntity(
            credentialId = UUID.randomUUID(), canonicalEmail = "expired@example.com",
            credentialDigest = digest, credentialKind = "LINK", issuedAt = now.minusSeconds(301),
            expiresAt = now.minusSeconds(1), remainingAttempts = 1
        ))

        assertEquals(0, credentialRepository.consumeIfActive(digest, now))
    }

    @Test
    fun `refresh rotation revokes the old session exactly once`() {
        val now = Instant.now()
        val sessionId = UUID.randomUUID()
        val replacementId = UUID.randomUUID()
        val familyId = UUID.randomUUID()
        sessionRepository.save(AuthSessionEntity(
            sessionId = sessionId, familyId = familyId, refreshTokenDigest = byteArrayOf(9, 10),
            createdAt = now, lastUsedAt = now, expiresAt = now.plus(30, ChronoUnit.DAYS),
            clientKind = "NATIVE"
        ))
        sessionRepository.save(AuthSessionEntity(
            sessionId = replacementId, familyId = familyId, refreshTokenDigest = byteArrayOf(11, 12),
            createdAt = now.plusSeconds(1), lastUsedAt = now.plusSeconds(1),
            expiresAt = now.plus(30, ChronoUnit.DAYS), clientKind = "NATIVE"
        ))

        assertEquals(1, sessionRepository.rotateIfActive(sessionId, replacementId, now.plusSeconds(1)))
        assertEquals(0, sessionRepository.rotateIfActive(sessionId, UUID.randomUUID(), now.plusSeconds(2)))
        assertEquals(1, sessionRepository.revokeFamily(familyId, now.plusSeconds(3)))
        assertEquals(0, sessionRepository.revokeFamily(familyId, now.plusSeconds(4)))
    }

    @Test
    fun `auth email outbox stores only protected delivery material`() {
        val now = Instant.now()
        val eventId = UUID.randomUUID()
        val record = authEmailOutboxRepository.save(AuthEmailOutboxEntity(
            outboxId = UUID.randomUUID(), eventId = eventId,
            recipient = "user@example.com", template = "LOGIN_LINK",
            encryptedCredential = "v1-protected-envelope", expiresAt = now.plusSeconds(600),
            createdAt = now, availableAt = now
        ))

        val loaded = authEmailOutboxRepository.findByEventId(eventId)
        assertEquals(record.encryptedCredential, loaded?.encryptedCredential)
        assertEquals("PENDING", loaded?.status)
    }

    @Test
    fun `auth email outbox claim leases the oldest event`() {
        val now = Instant.now()
        authEmailOutboxService.append(
            eventId = UUID.randomUUID(), recipient = "first@example.com",
            template = AuthEmailTemplate.LOGIN_CODE, encryptedCredential = "v1-a",
            expiresAt = now.plusSeconds(600), now = now
        )
        val claimed = authEmailOutboxService.claim(now.plusMillis(1), Duration.ofSeconds(30))

        assertEquals("CLAIMED", claimed?.status)
        assertEquals(1, claimed?.attempts)
        assertEquals("first@example.com", claimed?.recipient)
        assertEquals(null, authEmailOutboxService.claim(now.plusMillis(2), Duration.ofSeconds(30)))
    }

    @Test
    fun `claimed outbox event can be acknowledged only once`() {
        val now = Instant.now()
        val eventId = UUID.randomUUID()
        authEmailOutboxService.append(eventId, "ack@example.com", AuthEmailTemplate.LOGIN_LINK, "v1-a", now.plusSeconds(600), now)
        authEmailOutboxService.claim(now.plusMillis(1), Duration.ofSeconds(30))

        assertEquals(true, authEmailOutboxService.acknowledge(eventId, now.plusSeconds(2)))
        assertEquals(false, authEmailOutboxService.acknowledge(eventId, now.plusSeconds(3)))
        assertEquals("PUBLISHED", authEmailOutboxRepository.findByEventId(eventId)?.status)
    }

    @Test
    fun `rejected event is parked at attempt limit`() {
        val now = Instant.now()
        val eventId = UUID.randomUUID()
        authEmailOutboxService.append(eventId, "park@example.com", AuthEmailTemplate.LOGIN_CODE, "v1-p", now.plusSeconds(600), now)
        authEmailOutboxService.claim(now.plusMillis(1), Duration.ofSeconds(30))

        assertEquals(true, authEmailOutboxService.reject(eventId, now.plusSeconds(2), Duration.ZERO, 1))
        assertEquals("PARKED", authEmailOutboxRepository.findByEventId(eventId)?.status)
    }
}
