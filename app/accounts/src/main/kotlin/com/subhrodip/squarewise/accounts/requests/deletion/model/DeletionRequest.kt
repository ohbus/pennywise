package com.subhrodip.squarewise.accounts.requests.deletion.model

import java.time.Instant

/** Mutable domain state for one account deletion request. */
data class DeletionRequest(val subject: String, val requestedAt: Instant, var status: DeletionStatus = DeletionStatus.REQUESTED)
