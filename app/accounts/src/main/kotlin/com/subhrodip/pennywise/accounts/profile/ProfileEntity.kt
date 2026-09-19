package com.subhrodip.pennywise.accounts.profile

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Persistent JPA entity representing an account profile in the Accounts service.
 *
 * Mapped to table `account_profiles`.
 *
 * @property accountId Unique identifier of the account profile.
 * @property subject OIDC subject identifier associated with this profile.
 * @property displayName User-visible display name.
 * @property timezone User preferred timezone (e.g. "UTC").
 * @property defaultCurrency Three-letter ISO-4217 currency code.
 * @property deletionRequested Flag indicating if deletion has been requested for this profile.
 */
@Entity
@Table(name = "account_profiles")
class ProfileEntity(
    @Id
    @Column(name = "account_id", nullable = false)
    var accountId: UUID,

    @Column(name = "subject", nullable = false, unique = true, length = 200)
    var subject: String,

    @Column(name = "display_name", nullable = false, length = 120)
    var displayName: String,

    @Column(name = "timezone", nullable = false, length = 80)
    var timezone: String,

    @Column(name = "default_currency", nullable = false, length = 3)
    var defaultCurrency: String,

    @Column(name = "deletion_requested", nullable = false)
    var deletionRequested: Boolean = false
)
