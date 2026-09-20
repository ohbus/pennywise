package com.subhrodip.pennywise.accounts.auth.delivery

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies the provider-neutral authentication email message shape. */
class AuthEmailMessageTest {
    @Test
    fun `supports application-owned link and code templates`() {
        val expiry = Instant.parse("2026-09-20T00:10:00Z")
        val link = AuthEmailMessage("alice@example.com", AuthEmailTemplate.LOGIN_LINK, "opaque", expiry)
        val code = link.copy(template = AuthEmailTemplate.LOGIN_CODE)

        assertEquals(AuthEmailTemplate.LOGIN_LINK, link.template)
        assertEquals(AuthEmailTemplate.LOGIN_CODE, code.template)
        assertTrue(code.expiresAt.isAfter(Instant.parse("2026-09-20T00:00:00Z")))
    }
}
