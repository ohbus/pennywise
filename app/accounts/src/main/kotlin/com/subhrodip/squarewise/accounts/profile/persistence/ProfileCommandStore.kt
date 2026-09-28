package com.subhrodip.squarewise.accounts.profile.persistence

import com.subhrodip.squarewise.accounts.profile.api.ProfilePatchRequest
import com.subhrodip.squarewise.accounts.profile.api.ProfileResponse
/** Writer-side profile persistence port. */
interface ProfileCommandStore {
    /** Updates an existing profile. */
    fun update(subject: String, patch: ProfilePatchRequest): ProfileResponse
    /** Marks a profile for deletion. */
    fun requestDeletion(subject: String)
}
