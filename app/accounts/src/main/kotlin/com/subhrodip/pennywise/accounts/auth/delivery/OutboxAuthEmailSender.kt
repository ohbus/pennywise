package com.subhrodip.pennywise.accounts.auth.delivery

import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Outbox-backed implementation of [AuthEmailSender].
 *
 * Encrypts the raw one-time credential via [CredentialEnvelopeProtector] and appends
 * the delivery request to the local `auth_email_outbox` table in the same database transaction.
 *
 * @param outboxService Database outbox service.
 * @param envelopeProtector AES-GCM credential encryptor.
 */
@Component
class OutboxAuthEmailSender(
    private val outboxService: AuthEmailOutboxService,
    private val envelopeProtector: CredentialEnvelopeProtector
) : AuthEmailSender {

    override fun send(message: AuthEmailMessage): AuthEmailDeliveryResult {
        val context = CredentialDeliveryContext(
            recipient = message.recipient,
            template = message.template
        )
        val encryptedCredential = envelopeProtector.protect(message.credential, context)
        outboxService.append(
            eventId = UUID.randomUUID(),
            recipient = message.recipient,
            template = message.template,
            encryptedCredential = encryptedCredential,
            expiresAt = message.expiresAt,
            now = Instant.now()
        )
        return AuthEmailDeliveryResult.QUEUED
    }
}
