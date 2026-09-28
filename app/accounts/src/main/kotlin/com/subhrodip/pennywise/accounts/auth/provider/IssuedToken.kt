package com.subhrodip.pennywise.accounts.auth.provider

import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

/**
 * Encapsulates the issued token set.
 *
 * @property accessToken Signed bearer JWT access token.
 * @property tokenType Token scheme, standard "Bearer".
 * @property expiresIn Expiration lifetime of the access token in seconds.
 */
data class IssuedToken(
    val accessToken: String,
    val tokenType: String = ApiEndpoints.Headers.BEARER_SCHEME,
    val expiresIn: Long
)
