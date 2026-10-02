package com.subhrodip.squarewise.accounts.auth.identity.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [AccountIdentityEntity].
 */
@Repository
interface AccountIdentityRepository : JpaRepository<AccountIdentityEntity, UUID> {
    /**
     * Resolves an identity by its natural composite key of issuer and provider subject.
     *
     * @param issuer OIDC / authority issuer URI.
     * @param providerSubject Subject claim unique within that issuer.
     * @return Matching entity or null.
     */
    fun findByIssuerAndProviderSubject(issuer: String, providerSubject: String): AccountIdentityEntity?

    /**
     * Finds all identities associated with a given local account ID.
     *
     * @param accountId Unique account identifier.
     * @return List of mapped identities.
     */
    fun findByAccountId(accountId: UUID): List<AccountIdentityEntity>

    /**
     * Finds active identities matching a verified contact email (case-insensitive).
     *
     * @param email Normalized user email.
     * @param status Identity lifecycle status filter.
     * @return First matching identity or null.
     */
    fun findFirstByEmailIgnoreCaseAndStatus(email: String, status: String = "ACTIVE"): AccountIdentityEntity?
}
