package com.subhrodip.pennywise.accounts.profile.persistence

import com.subhrodip.pennywise.accounts.profile.api.ProfileResponse
import java.util.UUID

/** Reader-side profile persistence port. */
interface ProfileQueryStore {
    /** Retrieves a profile, applying the bounded-context default when absent. */
    fun get(subject: String): ProfileResponse
    /** Finds a profile by account id. */
    fun findById(accountId: UUID): ProfileResponse?
    /** Finds profiles for a batch of account ids. */
    fun findByIds(accountIds: List<UUID>): List<ProfileResponse>
}
