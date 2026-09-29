package com.subhrodip.squarewise.accounts.auth.identity.persistence

import com.subhrodip.squarewise.accounts.auth.identity.AccountIdentityStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Integration test verifying JPA-backed durable identity persistence,
 * uniqueness constraints, and email updates decoupled from subjects.
 */
@SpringBootTest
@Transactional
class JpaAccountIdentityStoreTest @Autowired constructor(
    private val identityStore: AccountIdentityStore,
    private val profileRepository: com.subhrodip.squarewise.accounts.profile.persistence.ProfileRepository
) {

    @Test
    fun `enrolls new identity and retrieves by accountId, composite key, and email`() {
        val accountId = UUID.randomUUID()
        val issuer = "https://accounts.squarewise.test"
        val subject = "sqw:$accountId"
        val email = "alice.smith@example.com"

        profileRepository.save(
            com.subhrodip.squarewise.accounts.profile.persistence.ProfileEntity(
                accountId = accountId,
                subject = subject,
                displayName = "Alice Smith",
                timezone = "UTC",
                defaultCurrency = "EUR"
            )
        )

        val enrolled = identityStore.enrollIdentity(
            accountId = accountId,
            issuer = issuer,
            providerSubject = subject,
            email = email,
            verified = true
        )

        assertEquals(accountId, enrolled.accountId)
        assertEquals(subject, enrolled.subject)
        assertEquals(email, enrolled.email)

        val byAccountId = identityStore.findByAccountId(accountId)
        assertNotNull(byAccountId)
        assertEquals(subject, byAccountId?.subject)

        val byComposite = identityStore.findByIssuerAndSubject(issuer, subject)
        assertNotNull(byComposite)
        assertEquals(accountId, byComposite?.accountId)

        val byEmail = identityStore.findByEmail("ALICE.SMITH@EXAMPLE.COM")
        assertNotNull(byEmail)
        assertEquals(accountId, byEmail?.accountId)
    }

    @Test
    fun `updates email without modifying account subject or issuer`() {
        val accountId = UUID.randomUUID()
        val issuer = "https://accounts.squarewise.test"
        val subject = "sqw:$accountId"
        val originalEmail = "bob.old@example.com"
        val newEmail = "bob.new@example.com"

        profileRepository.save(
            com.subhrodip.squarewise.accounts.profile.persistence.ProfileEntity(
                accountId = accountId,
                subject = subject,
                displayName = "Bob Jones",
                timezone = "UTC",
                defaultCurrency = "EUR"
            )
        )

        identityStore.enrollIdentity(
            accountId = accountId,
            issuer = issuer,
            providerSubject = subject,
            email = originalEmail,
            verified = true
        )

        val updated = identityStore.updateEmail(accountId, newEmail, verified = true)
        assertEquals(newEmail, updated.email)
        assertEquals(subject, updated.subject)

        assertNull(identityStore.findByEmail(originalEmail))
        val byNewEmail = identityStore.findByEmail(newEmail)
        assertNotNull(byNewEmail)
        assertEquals(accountId, byNewEmail?.accountId)
    }
}
