package com.subhrodip.pennywise.accounts

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.DateTimeException
import java.time.ZoneId

object ProfileRules {
    private val subjectPattern = Regex("^[A-Za-z0-9|._:-]{1,200}$")

    fun requireSubject(subject: String): String {
        if (!subjectPattern.matches(subject)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated subject is invalid")
        }
        return subject
    }

    fun requireTimezone(timezone: String): String {
        try {
            ZoneId.of(timezone)
        } catch (_: DateTimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "timezone must be a valid IANA zone")
        }
        return timezone
    }
}
