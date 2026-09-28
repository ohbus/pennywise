package com.subhrodip.pennywise.accounts.profile.api

import com.subhrodip.pennywise.accounts.requests.export.model.ExportStatus
import java.time.Instant
import java.util.UUID

/** Public status representation for an account export request. */
data class ExportRequestResponse(val exportId: UUID, val status: ExportStatus, val requestedAt: Instant)
