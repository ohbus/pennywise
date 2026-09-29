package com.subhrodip.squarewise.bff.config

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

/** Cookie names and bounded lifetime for the BFF-owned browser session. */
@ConfigurationProperties(prefix = "squarewise.security.browser.session")
data class BrowserSessionProperties(
    /** HttpOnly access-token cookie name. */
    var accessCookieName: String = "squarewise_access",
    /** HttpOnly rotating refresh-token cookie name. */
    var refreshCookieName: String = "squarewise_refresh",
    /** Readable CSRF nonce cookie name. */
    var csrfCookieName: String = "squarewise_csrf",
    /** Maximum browser cookie lifetime; never exceeds the server session policy. */
    var cookieMaxAge: Duration = Duration.ofDays(30)
)
