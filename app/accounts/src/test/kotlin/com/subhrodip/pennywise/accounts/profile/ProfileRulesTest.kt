package com.subhrodip.pennywise.accounts.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode

class ProfileRulesTest {
    @Test
    fun `accepts bounded subject and IANA timezone`() {
        assertEquals("oidc|alice", ProfileRules.requireSubject("oidc|alice"))
        assertEquals("Europe/Vienna", ProfileRules.requireTimezone("Europe/Vienna"))
    }

    @Test
    fun `rejects invalid subject and timezone with defined statuses`() {
        val ex = assertThrows(ApplicationException::class.java) {
            ProfileRules.requireSubject("alice with spaces")
        }
        assertEquals(ErrorCode.ERR_03, ex.errorCode)
        val ex2 = assertThrows(ApplicationException::class.java) {
            ProfileRules.requireTimezone("not/a-zone")
        }
        assertEquals(ErrorCode.ERR_02, ex2.errorCode)
    }
}
