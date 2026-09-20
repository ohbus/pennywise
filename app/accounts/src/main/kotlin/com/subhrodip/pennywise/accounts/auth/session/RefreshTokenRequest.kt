package com.subhrodip.pennywise.accounts.auth.session

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for rotating an active refresh token.
 *
 * Matches OpenAPI schema `RefreshTokenRequest`.
 *
 * @property refreshToken Cryptographically secure opaque refresh token.
 */
data class RefreshTokenRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 512)
    val refreshToken: String
)
