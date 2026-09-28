package com.subhrodip.pennywise.accounts.requests.export.model

import java.time.Instant
import java.util.UUID

/** Mutable domain state for one account data export request. */
data class ExportRequest(val exportId: UUID, val subject: String, val requestedAt: Instant, var status: ExportStatus = ExportStatus.REQUESTED)
