package com.subhrodip.pennywise.accounts.auth.delivery.model

import java.time.Instant

/** Provider-neutral authentication email data passed to an outbound adapter. */
data class AuthEmailMessage(
    val recipient: String,
    val template: AuthEmailTemplate,
    val credential: String,
    val expiresAt: Instant
)
