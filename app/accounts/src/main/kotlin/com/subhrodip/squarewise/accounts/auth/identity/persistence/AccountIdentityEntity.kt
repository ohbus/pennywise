package com.subhrodip.squarewise.accounts.auth.identity.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Persistent JPA entity representing a durable account identity binding.
 *
 * Mapped to table `account_identities`. Represents the authoritative `(issuer, provider_subject)`
 * authentication mapping decoupled from mutable email contact information.
 *
 * @property identityId Unique identifier of this identity binding.
 * @property accountId The owning account's UUID (foreign key to `account_profiles`).
 * @property issuer Trusted authority/issuer identifier (e.g. issuer URI).
 * @property providerSubject Provider-specific unique subject identifier.
 * @property email Optional normalized email associated with this identity.
 * @property emailVerified Whether the contact email has been verified.
 * @property status Current lifecycle status of the identity (e.g., "ACTIVE", "SUSPENDED").
 * @property createdAt Timestamp when the identity was initially enrolled.
 * @property updatedAt Timestamp of the last identity state change.
 */
@Entity
@Table(name = "account_identities")
class AccountIdentityEntity(
    @Id
    @Column(name = "identity_id", nullable = false)
    var identityId: UUID,

    @Column(name = "account_id", nullable = false)
    var accountId: UUID,

    @Column(name = "issuer", nullable = false, length = 255)
    var issuer: String,

    @Column(name = "provider_subject", nullable = false, length = 255)
    var providerSubject: String,

    @Column(name = "email", length = 255)
    var email: String? = null,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "status", nullable = false, length = 32)
    var status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
