package com.subhrodip.squarewise.accounts.auth.identity

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Specifies the default identity-store email-update contract and fail-closed lookup. */
class AccountIdentityStoreTest {
    @Test
    fun `default updateEmail returns the existing durable identity`() {
        val accountId = UUID.randomUUID()
        val identity = AccountIdentity(
            accountId = accountId,
            subject = "oidc|subject",
            email = "old@example.test",
            deletionRequested = false,
            issuer = "https://issuer.example.test"
        )
        val store = object : AccountIdentityStore {
            override fun findByAccountId(requestedAccountId: UUID): AccountIdentity? =
                identity.takeIf { it.accountId == requestedAccountId }
        }

        assertEquals(identity, store.updateEmail(accountId, "new@example.test", verified = true))
    }

    @Test
    fun `default updateEmail rejects an unmapped account`() {
        val store = object : AccountIdentityStore {
            override fun findByAccountId(accountId: UUID): AccountIdentity? = null
        }

        val error = assertFailsWith<IllegalArgumentException> {
            store.updateEmail(UUID.randomUUID(), "new@example.test", verified = true)
        }

        assertEquals("Account not found", error.message)
    }
}
