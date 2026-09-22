package com.subhrodip.pennywise.accounts.profile.persistence

import com.subhrodip.pennywise.accounts.profile.model.StoredProfile
import com.subhrodip.pennywise.accounts.profile.api.ProfileResponse
import com.subhrodip.pennywise.accounts.profile.service.ProfileRules
import com.subhrodip.pennywise.accounts.profile.api.ProfilePatchRequest
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe implementation of [ProfileStore] used for unit testing.
 */
class InMemoryProfileStore : ProfileStore {
    private val profiles = ConcurrentHashMap<String, StoredProfile>()

    override fun get(subject: String): ProfileResponse {
        ProfileRules.requireSubject(subject)
        return profiles.computeIfAbsent(subject) { default(subject) }.response
    }

    override fun findById(accountId: UUID): ProfileResponse? =
        profiles.values.firstOrNull { it.response.accountId == accountId }?.response

    override fun findByIds(accountIds: List<UUID>): List<ProfileResponse> =
        profiles.values.filter { accountIds.contains(it.response.accountId) }.map { it.response }

    override fun update(subject: String, patch: ProfilePatchRequest): ProfileResponse {
        val current = get(subject)
        val updated = current.copy(
            displayName = patch.displayName ?: current.displayName,
            timezone = patch.timezone?.also(ProfileRules::requireTimezone) ?: current.timezone,
            defaultCurrency = patch.defaultCurrency ?: current.defaultCurrency
        )
        profiles[subject] = StoredProfile(updated)
        return updated
    }

    override fun requestDeletion(subject: String) {
        val current = profiles.computeIfAbsent(subject) { default(subject) }
        profiles[subject] = current.copy(deletionRequested = true)
    }

    private fun default(subject: String) = StoredProfile(
        ProfileResponse(
            UUID.nameUUIDFromBytes(subject.toByteArray(StandardCharsets.UTF_8)),
            subject,
            "UTC",
            "EUR"
        )
    )
}
