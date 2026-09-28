package com.subhrodip.squarewise.accounts.profile.model

import com.subhrodip.squarewise.accounts.profile.api.ProfileResponse
/** In-memory profile state including the deletion-request marker. */
data class StoredProfile(val response: ProfileResponse, var deletionRequested: Boolean = false)
