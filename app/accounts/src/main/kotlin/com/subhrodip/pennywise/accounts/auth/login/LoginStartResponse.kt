package com.subhrodip.pennywise.accounts.auth.login

/**
 * Response payload for passwordless login initiation.
 *
 * Matches OpenAPI schema `LoginStartResponse`.
 *
 * @property status Fixed status string, always "ACCEPTED".
 */
data class LoginStartResponse(
    val status: String = "ACCEPTED"
)
