package com.subhrodip.pennywise.observability

/** Redacts credentials and masks personal data before values reach logs. */
object SensitiveDataSanitizer {
    /** Masks an email local part while retaining its domain for diagnostics. */
    fun email(value: String): String {
        val separator = value.indexOf('@')
        if (separator <= 0 || separator == value.lastIndex) return "[REDACTED]"
        return "${value.first()}***${value.substring(separator)}"
    }

    /** Replaces a bearer token while preserving the authentication scheme. */
    fun token(value: String): String =
        if (value.trimStart().startsWith("Bearer ", ignoreCase = true)) "Bearer [REDACTED]" else "[REDACTED]"
}
