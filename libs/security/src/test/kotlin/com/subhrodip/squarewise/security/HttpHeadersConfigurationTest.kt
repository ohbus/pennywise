package com.subhrodip.squarewise.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/** Verifies the default security header constant policies. */
class HttpHeadersConfigurationTest {

    @Test
    fun `verifies security headers configuration constants`() {
        assertEquals("default-src 'none'; frame-ancestors 'none'; sandbox", HttpHeadersConfiguration.DEFAULT_REST_CSP)
        assertEquals("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'", HttpHeadersConfiguration.DEFAULT_BFF_CSP)
        assertEquals("camera=(), microphone=(), geolocation=(), payment=()", HttpHeadersConfiguration.DEFAULT_PERMISSIONS_POLICY)
        assertEquals(31536000L, HttpHeadersConfiguration.HSTS_MAX_AGE_SECONDS)
    }
}
