package com.subhrodip.squarewise.accounts.auth.identity

import java.util.UUID

/**
 * Trusted account identity used when issuing a replacement access token.
 *
 * The provider-qualified subject is the authorization identity. The email is
 * auxiliary token data and must never be used as the durable account key.
 *
 * @property accountId Stable local account identifier.
 * @property subject Provider-qualified subject claim.
 * @property email Canonical contact value used only when a provider requires it.
 * @property deletionRequested Whether the account has entered deletion workflow.
 */
data class AccountIdentity(
    val accountId: UUID,
    val subject: String,
    val email: String,
    val deletionRequested: Boolean
)
