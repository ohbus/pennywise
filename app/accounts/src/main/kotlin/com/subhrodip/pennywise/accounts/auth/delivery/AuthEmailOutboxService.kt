package com.subhrodip.pennywise.accounts.auth.delivery

import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Coordinates durable auth-email outbox append and single-worker leasing. */
@Service
class AuthEmailOutboxService(
    private val repository: AuthEmailOutboxRepository
) {
    /** Appends one delivery event with an idempotent event identifier. */
    @Transactional
    fun append(eventId: UUID, recipient: String, template: AuthEmailTemplate, encryptedCredential: String, expiresAt: Instant, now: Instant): AuthEmailOutboxEntity = repository.save(
        AuthEmailOutboxEntity(UUID.randomUUID(), eventId, recipient, template.name, encryptedCredential, expiresAt, now, now)
    )

    /** Claims the oldest available event for a bounded worker lease. */
    @Transactional
    fun claim(now: Instant, lease: Duration): AuthEmailOutboxEntity? {
        require(!lease.isNegative && !lease.isZero) { "lease must be positive" }
        val record = repository.findAvailableForClaim(now).firstOrNull() ?: return null
        record.status = "CLAIMED"
        record.availableAt = now.plus(lease)
        record.attempts += 1
        return repository.save(record)
    }
}
