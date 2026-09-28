package com.subhrodip.squarewise.accounts.auth.delivery.model

/** Result of one bounded authentication-email publisher poll. */
enum class AuthEmailPublishOutcome { EMPTY, PUBLISHED, RETRY_SCHEDULED }
