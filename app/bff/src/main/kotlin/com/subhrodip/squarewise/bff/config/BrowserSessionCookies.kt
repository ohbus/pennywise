package com.subhrodip.squarewise.bff.config

import java.time.Duration
import org.springframework.http.ResponseCookie

/** Creates the BFF-owned browser cookies with explicit security attributes. */
class BrowserSessionCookies(private val properties: BrowserSessionProperties) {
    /** Creates the HttpOnly access-token cookie. */
    fun access(value: String): ResponseCookie = secureCookie(properties.accessCookieName, value, "/")

    /** Creates the HttpOnly rotating refresh-token cookie. */
    fun refresh(value: String): ResponseCookie = secureCookie(properties.refreshCookieName, value, "/auth")

    /** Creates the readable nonce cookie used by the double-submit CSRF check. */
    fun csrf(value: String): ResponseCookie = ResponseCookie.from(properties.csrfCookieName, value)
        .secure(true)
        .httpOnly(false)
        .sameSite("Lax")
        .path("/auth")
        .maxAge(properties.cookieMaxAge)
        .build()

    /** Creates an expired cookie that removes a browser session value. */
    fun clear(name: String, path: String): ResponseCookie = ResponseCookie.from(name, "")
        .secure(true)
        .httpOnly(name != properties.csrfCookieName)
        .sameSite("Lax")
        .path(path)
        .maxAge(Duration.ZERO)
        .build()

    private fun secureCookie(name: String, value: String, path: String): ResponseCookie =
        ResponseCookie.from(name, value)
            .secure(true)
            .httpOnly(true)
            .sameSite("Lax")
            .path(path)
            .maxAge(properties.cookieMaxAge)
            .build()
}
