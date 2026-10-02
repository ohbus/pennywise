package com.subhrodip.squarewise.accounts.auth.session

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Verifies the ordering invariant that bounds refresh-session expiry evaluation. */
class SessionExpiryTest {
    private val base = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `equal idle and absolute boundaries are valid`() {
        val expiry = SessionExpiry(base, base)

        assertEquals(base, expiry.idleExpiresAt)
        assertEquals(base, expiry.absoluteExpiresAt)
    }

    @Test
    fun `idle boundary cannot exceed absolute boundary`() {
        val error = assertFailsWith<IllegalArgumentException> {
            SessionExpiry(base.plusSeconds(1), base)
        }

        assertEquals("Idle expiry must not be later than absolute expiry", error.message)
    }
}
