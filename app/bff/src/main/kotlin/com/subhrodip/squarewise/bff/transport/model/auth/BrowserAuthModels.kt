package com.subhrodip.squarewise.bff.transport.model.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Browser login-start payload forwarded to the Accounts service. */
data class BrowserLoginStartRequest(
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String,
    val channel: String? = null
)

/** Stable accepted response returned by the Accounts login-start endpoint. */
data class BrowserLoginStartResponse(
    val status: String,
    val retryAfterSeconds: Long? = null
)

/** Browser login-verification payload; the BFF always supplies the browser client kind. */
data class BrowserLoginVerifyRequest(@field:NotBlank @field:Size(max = 512) val credential: String)

/** Token response received from Accounts and retained only inside the BFF boundary. */
data class AccountsTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshToken: String
)

/** Browser-safe response; access and refresh credentials remain cookie-contained. */
data class BrowserSessionResponse(val status: String = "AUTHENTICATED", val expiresIn: Long)
