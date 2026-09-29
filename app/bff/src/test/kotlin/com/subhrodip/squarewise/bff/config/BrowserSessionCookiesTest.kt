package com.subhrodip.squarewise.bff.config

import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies that browser credentials receive the required RFC 6265 cookie attributes. */
class BrowserSessionCookiesTest {
    private val cookies = BrowserSessionCookies(
        BrowserSessionProperties(cookieMaxAge = Duration.ofDays(30))
    )

    @Test
    fun `credential cookies are secure httpOnly and scoped`() {
        val access = cookies.access("access")
        val refresh = cookies.refresh("refresh")

        assertTrue(access.isSecure)
        assertTrue(access.isHttpOnly)
        assertEquals("Lax", access.sameSite)
        assertEquals("/", access.path)
        assertEquals(Duration.ofDays(30), access.maxAge)
        assertTrue(refresh.isSecure)
        assertTrue(refresh.isHttpOnly)
        assertEquals("Lax", refresh.sameSite)
        assertEquals("/auth", refresh.path)
    }

    @Test
    fun `csrf cookie is readable but clearing it is explicit`() {
        val csrf = cookies.csrf("nonce")
        val cleared = cookies.clear("squarewise_csrf", "/auth")

        assertTrue(csrf.isSecure)
        assertFalse(csrf.isHttpOnly)
        assertEquals("Lax", csrf.sameSite)
        assertEquals("/auth", csrf.path)
        assertEquals(Duration.ZERO, cleared.maxAge)
        assertEquals("/auth", cleared.path)
    }
}
