package com.subhrodip.pennywise.accounts

import java.util.UUID

/**
 * Domain port for reading and modifying user profiles.
 */
interface ProfileStore {
    /**
     * Retrieves the profile corresponding to the given subject, defaulting if not found.
     */
    fun get(subject: String): ProfileResponse

    /**
     * Finds a profile by account ID.
     */
    fun findById(accountId: UUID): ProfileResponse?

    /**
     * Finds profiles by a batch of account IDs.
     */
    fun findByIds(accountIds: List<UUID>): List<ProfileResponse>

    /**
     * Updates an existing profile using the patch payload.
     */
    fun update(subject: String, patch: ProfilePatchRequest): ProfileResponse

    /**
     * Marks the profile for deletion.
     */
    fun requestDeletion(subject: String)
}
