package com.subhrodip.pennywise.accounts.requests.export

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Persistent JPA entity representing an account data export request.
 *
 * Mapped to table `account_export_requests`.
 *
 * @property exportId Unique identifier of the export request.
 * @property subject Unique OIDC subject identifier for the requesting account.
 * @property status Current status of the export request ([ExportStatus]).
 * @property requestedAt Timestamp when the export request was initiated.
 */
@Entity
@Table(name = "account_export_requests")
class AccountExportRequestEntity(
    @Id
    @Column(name = "export_id", nullable = false)
    var exportId: UUID,

    @Column(name = "subject", nullable = false, length = 200)
    var subject: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: ExportStatus = ExportStatus.REQUESTED,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = Instant.now()
)
