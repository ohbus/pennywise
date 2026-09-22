package com.subhrodip.pennywise.accounts.profile.model

import com.subhrodip.pennywise.accounts.profile.api.ProfileResponse
/** In-memory profile state including the deletion-request marker. */
data class StoredProfile(val response: ProfileResponse, var deletionRequested: Boolean = false)
