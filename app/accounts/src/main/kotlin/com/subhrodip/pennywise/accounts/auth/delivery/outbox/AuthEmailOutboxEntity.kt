package com.subhrodip.pennywise.accounts.auth.delivery.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Accounts-owned durable handoff record; only encrypted credential material is stored. */
@Entity
@Table(name = "auth_email_outbox")
class AuthEmailOutboxEntity(
    @Id @Column(name = "outbox_id", nullable = false)
    var outboxId: UUID,
    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: UUID,
    @Column(name = "recipient", nullable = false, length = 254)
    var recipient: String,
    @Column(name = "template", nullable = false, length = 16)
    var template: String,
    @Column(name = "encrypted_credential", nullable = false, length = 4096)
    var encryptedCredential: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "available_at", nullable = false)
    var availableAt: Instant,
    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,
    @Column(name = "status", nullable = false, length = 16)
    var status: String = "PENDING"
)
