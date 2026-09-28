package com.subhrodip.squarewise.accounts.auth.delivery.model

/** Generic delivery outcome independent of account state. */
enum class AuthEmailDeliveryResult { QUEUED, RETRYABLE_FAILURE, PERMANENT_FAILURE }
