package com.subhrodip.pennywise.accounts.auth.login

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request body for redeeming a single-use login credential.
 *
 * Matches OpenAPI schema `LoginVerifyRequest`.
 *
 * @property credential Plaintext one-time link token or numeric code.
 * @property clientKind Client application type ("BROWSER" or "NATIVE").
 */
data class LoginVerifyRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 512)
    val credential: String,
    @field:NotBlank
    @field:Pattern(regexp = "^(BROWSER|NATIVE)$", message = "clientKind must be BROWSER or NATIVE")
    val clientKind: String
)
