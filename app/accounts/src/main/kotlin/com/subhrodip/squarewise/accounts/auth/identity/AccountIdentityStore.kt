package com.subhrodip.squarewise.accounts.auth.identity

import java.util.UUID

/** Writer-authoritative lookup port for trusted token-issuance identity data. */
fun interface AccountIdentityStore {
    /**
     * Finds the current identity for a durable account.
     *
     * @param accountId Local account identifier stored in the session.
     * @return Current identity or `null` when the account is unmapped.
     */
    fun findByAccountId(accountId: UUID): AccountIdentity?
}
