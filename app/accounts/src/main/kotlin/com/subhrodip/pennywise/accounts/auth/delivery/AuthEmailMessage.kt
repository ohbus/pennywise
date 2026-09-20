package com.subhrodip.pennywise.accounts.auth.delivery

import java.time.Instant

/** Provider-neutral authentication email data passed to an outbound adapter. */
data class AuthEmailMessage(
    val recipient: String,
    val template: AuthEmailTemplate,
    val credential: String,
    val expiresAt: Instant
)

/** Authentication email templates supported by the application-owned UX. */
enum class AuthEmailTemplate { LOGIN_LINK, LOGIN_CODE }

/** Generic delivery outcome intentionally independent of account state. */
enum class AuthEmailDeliveryResult { QUEUED, RETRYABLE_FAILURE, PERMANENT_FAILURE }
