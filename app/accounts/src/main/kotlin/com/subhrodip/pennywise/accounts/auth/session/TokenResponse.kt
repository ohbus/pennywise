package com.subhrodip.pennywise.accounts.auth.session

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

/**
 * Public response model for issued and rotated token bundles.
 *
 * Exactly matches OpenAPI schema `TokenResponse`.
 *
 * @property accessToken Signed bearer JWT access token.
 * @property tokenType Authorization scheme, always "Bearer".
 * @property expiresIn Lifespan of access token in seconds.
 * @property refreshToken Cryptographically secure opaque refresh token.
 */
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = ApiEndpoints.Headers.BEARER_SCHEME,
    val expiresIn: Long,
    val refreshToken: String
)
