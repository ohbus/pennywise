package com.subhrodip.pennywise.accounts.auth.login

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for starting a passwordless login flow.
 *
 * Matches OpenAPI schema `LoginStartRequest`.
 *
 * @property email User's email address.
 * @property channel Optional preferred delivery channel ("LINK" or "CODE").
 * @property clientKind Optional requesting client environment ("BROWSER" or "NATIVE").
 */
data class LoginStartRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val email: String,
    val channel: String? = null,
    val clientKind: String? = null
)
