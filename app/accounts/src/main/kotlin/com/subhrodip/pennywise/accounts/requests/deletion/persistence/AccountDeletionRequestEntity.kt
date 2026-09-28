package com.subhrodip.pennywise.accounts.requests.deletion.persistence

import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Persistent JPA entity representing an account deletion request.
 *
 * Mapped to table `account_deletion_requests`.
 *
 * @property subject Unique OIDC subject identifier for the account requested for deletion.
 * @property status Current status of the deletion request ([DeletionStatus]).
 * @property requestedAt Timestamp when the deletion request was initiated.
 */
@Entity
@Table(name = "account_deletion_requests")
class AccountDeletionRequestEntity(
    @Id
    @Column(name = "subject", nullable = false, length = 200)
    var subject: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: DeletionStatus = DeletionStatus.REQUESTED,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = Instant.now()
)
