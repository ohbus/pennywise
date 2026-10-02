package com.subhrodip.squarewise.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity

/** Verifies the default security header constant policies. */
class HttpHeadersConfigurationTest {

    @Test
    fun `verifies security headers configuration constants`() {
        assertEquals("default-src 'none'; frame-ancestors 'none'; sandbox", HttpHeadersConfiguration.DEFAULT_REST_CSP)
        assertEquals("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'", HttpHeadersConfiguration.DEFAULT_BFF_CSP)
        assertEquals("camera=(), microphone=(), geolocation=(), payment=()", HttpHeadersConfiguration.DEFAULT_PERMISSIONS_POLICY)
        assertEquals(31536000L, HttpHeadersConfiguration.HSTS_MAX_AGE_SECONDS)
    }

    @Test
    fun `applies servlet security headers to the public builder`() {
        @Suppress("UNCHECKED_CAST")
        val postProcessor = mock(ObjectPostProcessor::class.java) as ObjectPostProcessor<Any>
        val http = HttpSecurity(
            postProcessor,
            mock(AuthenticationManagerBuilder::class.java),
            mutableMapOf()
        )

        assertEquals(http, HttpHeadersConfiguration.applyServletSecurityHeaders(http))
    }

    @Test
    fun `applies reactive security headers to the public builder`() {
        val http = ServerHttpSecurity.http()

        assertEquals(http, HttpHeadersConfiguration.applyReactiveSecurityHeaders(http))
    }
}
