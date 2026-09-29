package com.subhrodip.squarewise.accounts.auth.session

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Request that identifies the caller's opaque refresh session for revocation. */
data class LogoutRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 512)
    val refreshToken: String
)
