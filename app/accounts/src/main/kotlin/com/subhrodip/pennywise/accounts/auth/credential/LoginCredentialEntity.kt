package com.subhrodip.pennywise.accounts.auth.credential

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Persisted one-time login credential; plaintext is never represented here. */
@Entity
@Table(name = "auth_login_credentials")
class LoginCredentialEntity(
    @Id
    @Column(name = "credential_id", nullable = false)
    var credentialId: UUID,
    @Column(name = "canonical_email", nullable = false, length = 254)
    var canonicalEmail: String,
    @Column(name = "credential_digest", nullable = false, unique = true)
    var credentialDigest: ByteArray,
    @Column(name = "credential_kind", nullable = false, length = 16)
    var credentialKind: String,
    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "remaining_attempts", nullable = false)
    var remainingAttempts: Int,
    @Column(name = "consumed_at")
    var consumedAt: Instant? = null
)
