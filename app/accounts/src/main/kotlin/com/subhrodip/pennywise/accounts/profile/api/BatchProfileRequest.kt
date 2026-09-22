package com.subhrodip.pennywise.accounts.profile.api

import jakarta.validation.constraints.Size
import java.util.UUID

/** Bounded batch profile lookup input. */
data class BatchProfileRequest(@field:Size(min = 1, max = 100) val accountIds: List<UUID>)
