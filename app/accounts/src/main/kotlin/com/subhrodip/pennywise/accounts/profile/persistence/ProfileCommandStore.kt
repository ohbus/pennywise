package com.subhrodip.pennywise.accounts.profile.persistence

import com.subhrodip.pennywise.accounts.profile.api.ProfilePatchRequest
import com.subhrodip.pennywise.accounts.profile.api.ProfileResponse
/** Writer-side profile persistence port. */
interface ProfileCommandStore {
    /** Updates an existing profile. */
    fun update(subject: String, patch: ProfilePatchRequest): ProfileResponse
    /** Marks a profile for deletion. */
    fun requestDeletion(subject: String)
}
