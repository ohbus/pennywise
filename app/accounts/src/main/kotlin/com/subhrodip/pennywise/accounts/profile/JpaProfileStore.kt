package com.subhrodip.pennywise.accounts.profile

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * JPA-backed implementation of [ProfileStore] for managing user account profiles.
 *
 * Interacts with PostgreSQL through [ProfileRepository]. Defaults and persists new profiles
 * upon initial lookup if absent.
 */
@Primary
@Service
class JpaProfileStore(private val repository: ProfileRepository) : ProfileStore {
    /**
     * Retrieves the profile associated with the given subject, provisioning a default profile if none exists.
     *
     * @param subject OIDC subject identifier. Must not be blank.
     * @return [ProfileResponse] representing the profile.
     */
    override fun get(subject: String): ProfileResponse {
        ProfileRules.requireSubject(subject)
        return (repository.findBySubject(subject)
            ?: repository.save(default(subject))).toResponse()
    }

    /**
     * Finds a profile by account ID.
     *
     * @param accountId Unique identifier of the profile.
     * @return [ProfileResponse] or null if not found.
     */
    override fun findById(accountId: UUID): ProfileResponse? =
        repository.findById(accountId).orElse(null)?.toResponse()

    /**
     * Finds profiles by a batch of account IDs.
     *
     * @param accountIds List of unique account profile UUIDs.
     * @return List of matching [ProfileResponse]s.
     */
    override fun findByIds(accountIds: List<UUID>): List<ProfileResponse> =
        repository.findAllById(accountIds).map { it.toResponse() }

    /**
     * Updates an existing profile or defaults and updates if absent.
     *
     * @param subject OIDC subject identifier.
     * @param patch Patch request containing fields to update.
     * @return Updated [ProfileResponse].
     */
    override fun update(subject: String, patch: ProfilePatchRequest): ProfileResponse {
        ProfileRules.requireSubject(subject)
        val entity = repository.findBySubject(subject) ?: default(subject)
        entity.displayName = patch.displayName ?: entity.displayName
        entity.timezone = patch.timezone?.also(ProfileRules::requireTimezone) ?: entity.timezone
        entity.defaultCurrency = patch.defaultCurrency ?: entity.defaultCurrency
        return repository.save(entity).toResponse()
    }

    /**
     * Marks an account profile for deletion while preserving historical financial attribution.
     *
     * @param subject OIDC subject identifier.
     */
    override fun requestDeletion(subject: String) {
        ProfileRules.requireSubject(subject)
        val entity = repository.findBySubject(subject) ?: default(subject)
        entity.deletionRequested = true
        repository.save(entity)
    }

    private fun default(subject: String) = ProfileEntity(
        UUID.nameUUIDFromBytes(subject.toByteArray(StandardCharsets.UTF_8)), subject, subject, "UTC", "EUR"
    )
}

/**
 * Maps [ProfileEntity] to its public [ProfileResponse] representation.
 */
private fun ProfileEntity.toResponse() = ProfileResponse(accountId, displayName, timezone, defaultCurrency)
