package com.subhrodip.pennywise.accounts.auth.session

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Persisted refresh-token family member; raw refresh tokens are never stored. */
@Entity
@Table(name = "auth_sessions")
class AuthSessionEntity(
    @Id
    @Column(name = "session_id", nullable = false)
    var sessionId: UUID,
    @Column(name = "account_id")
    var accountId: UUID? = null,
    @Column(name = "family_id", nullable = false)
    var familyId: UUID,
    @Column(name = "refresh_token_digest", nullable = false, unique = true)
    var refreshTokenDigest: ByteArray,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
    @Column(name = "replaced_by_session_id")
    var replacedBySessionId: UUID? = null,
    @Column(name = "device_label", length = 120)
    var deviceLabel: String? = null,
    @Column(name = "client_kind", nullable = false, length = 16)
    var clientKind: String
)
