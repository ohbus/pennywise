package com.subhrodip.squarewise.accounts.auth.delivery.service

import com.subhrodip.squarewise.accounts.auth.delivery.model.AuthEmailDeliveryResult
import com.subhrodip.squarewise.accounts.auth.delivery.model.AuthEmailMessage
import com.subhrodip.squarewise.accounts.auth.delivery.model.AuthEmailTemplate
import com.subhrodip.squarewise.accounts.auth.delivery.model.CredentialDeliveryContext
import com.subhrodip.squarewise.accounts.auth.delivery.outbox.AuthEmailOutboxEntity
import com.subhrodip.squarewise.accounts.auth.delivery.outbox.AuthEmailOutboxRepository
import com.subhrodip.squarewise.accounts.auth.delivery.security.CredentialEnvelopeProtector
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.doAnswer

/** Verifies encryption and durable handoff invariants of the auth-email sender. */
class OutboxAuthEmailSenderTest {

    private val expiresAt = Instant.parse("2026-09-30T00:00:00Z")
    private val message = AuthEmailMessage(
        recipient = "person@example.com",
        template = AuthEmailTemplate.LOGIN_CODE,
        credential = "raw-credential",
        expiresAt = expiresAt
    )

    /** Protects with recipient/template AAD and appends only the protected value. */
    @Test
    fun `protects credential with delivery context before queueing`() {
        val repository = mock(AuthEmailOutboxRepository::class.java)
        var saved: AuthEmailOutboxEntity? = null
        doAnswer { invocation ->
            saved = invocation.getArgument(0)
            saved
        }.`when`(repository).save(any(AuthEmailOutboxEntity::class.java))
        val protector = RecordingProtector()

        val result = sender(AuthEmailOutboxService(repository), protector).send(message)

        assertEquals(AuthEmailDeliveryResult.QUEUED, result)
        assertEquals("raw-credential", protector.plaintext)
        assertEquals(
            CredentialDeliveryContext(message.recipient, message.template),
            protector.context
        )

        val record = requireNotNull(saved)
        assertEquals(message.recipient, record.recipient)
        assertEquals(message.template.name, record.template)
        assertEquals("v1-protected", record.encryptedCredential)
        assertEquals(message.expiresAt, record.expiresAt)
        assertTrue(record.encryptedCredential != message.credential)
    }

    /** Propagates append failure and does not report a queued delivery. */
    @Test
    fun `append failure prevents queued result`() {
        val repository = mock(AuthEmailOutboxRepository::class.java)
        doThrow(IllegalStateException("database unavailable")).`when`(repository).save(
            any(AuthEmailOutboxEntity::class.java)
        )

        val error = assertThrows(IllegalStateException::class.java) {
            sender(AuthEmailOutboxService(repository), RecordingProtector()).send(message)
        }

        assertEquals("database unavailable", error.message)
    }

    private fun sender(
        outbox: AuthEmailOutboxService,
        protector: CredentialEnvelopeProtector
    ): OutboxAuthEmailSender = OutboxAuthEmailSender(outbox, protector)

    private class RecordingProtector : CredentialEnvelopeProtector {
        var plaintext: String? = null
        var context: CredentialDeliveryContext? = null

        override fun protect(
            plaintext: String,
            context: CredentialDeliveryContext
        ): String {
            this.plaintext = plaintext
            this.context = context
            return "v1-protected"
        }

        override fun reveal(
            envelope: String,
            context: CredentialDeliveryContext
        ): String = error("reveal is not used by this test")
    }
}
