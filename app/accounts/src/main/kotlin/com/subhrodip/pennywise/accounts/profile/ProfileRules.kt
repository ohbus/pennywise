package com.subhrodip.pennywise.accounts.profile

import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.time.DateTimeException
import java.time.ZoneId

object ProfileRules {
    private val subjectPattern = Regex("^[A-Za-z0-9|._:@-]{1,200}$")

    fun requireSubject(subject: String): String {
        if (!subjectPattern.matches(subject)) {
            throw ApplicationException(ErrorCode.ERR_03, "Authenticated subject is invalid")
        }
        return subject
    }

    fun requireTimezone(timezone: String): String {
        try {
            ZoneId.of(timezone)
        } catch (_: DateTimeException) {
            throw ApplicationException(ErrorCode.ERR_02, "timezone must be a valid IANA zone")
        }
        return timezone
    }
}
