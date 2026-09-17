package com.subhrodip.pennywise.accounts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException

class ProfileRulesTest {
    @Test
    fun `accepts bounded subject and IANA timezone`() {
        assertEquals("oidc|alice", ProfileRules.requireSubject("oidc|alice"))
        assertEquals("Europe/Vienna", ProfileRules.requireTimezone("Europe/Vienna"))
    }

    @Test
    fun `rejects invalid subject and timezone with defined statuses`() {
        assertEquals(401, assertThrows(ResponseStatusException::class.java) {
            ProfileRules.requireSubject("alice with spaces")
        }.statusCode.value())
        assertEquals(400, assertThrows(ResponseStatusException::class.java) {
            ProfileRules.requireTimezone("not/a-zone")
        }.statusCode.value())
    }
}
