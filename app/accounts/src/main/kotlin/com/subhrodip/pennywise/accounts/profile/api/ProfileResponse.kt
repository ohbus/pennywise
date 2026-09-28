package com.subhrodip.pennywise.accounts.profile.api

import java.util.UUID

/** Public account profile representation returned by the Accounts API. */
data class ProfileResponse(val accountId: UUID, val displayName: String, val timezone: String, val defaultCurrency: String)
