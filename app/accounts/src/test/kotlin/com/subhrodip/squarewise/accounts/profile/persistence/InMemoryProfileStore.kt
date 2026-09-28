package com.subhrodip.squarewise.accounts.profile.persistence

import com.subhrodip.squarewise.accounts.profile.model.StoredProfile
import com.subhrodip.squarewise.accounts.auth.identity.AccountIdentity
import com.subhrodip.squarewise.accounts.auth.identity.AccountIdentityStore
import com.subhrodip.squarewise.accounts.profile.api.ProfileResponse
import com.subhrodip.squarewise.accounts.profile.service.ProfileRules
import com.subhrodip.squarewise.accounts.profile.api.ProfilePatchRequest
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe implementation of [ProfileStore] used for unit testing.
 */
class InMemoryProfileStore : ProfileStore, AccountIdentityStore {
    private val profiles = ConcurrentHashMap<String, StoredProfile>()

    override fun get(subject: String): ProfileResponse {
        ProfileRules.requireSubject(subject)
        return profiles.computeIfAbsent(subject) { default(subject) }.response
    }

    override fun findById(accountId: UUID): ProfileResponse? =
        profiles.values.firstOrNull { it.response.accountId == accountId }?.response

    override fun findByIds(accountIds: List<UUID>): List<ProfileResponse> =
        profiles.values.filter { accountIds.contains(it.response.accountId) }.map { it.response }

    override fun findByAccountId(accountId: UUID): AccountIdentity? =
        profiles.entries.firstOrNull { it.value.response.accountId == accountId }?.let { (subject, profile) ->
            AccountIdentity(
                accountId = accountId,
                subject = subject,
                email = subject.removePrefix("internal:"),
                deletionRequested = profile.deletionRequested
            )
        }

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
