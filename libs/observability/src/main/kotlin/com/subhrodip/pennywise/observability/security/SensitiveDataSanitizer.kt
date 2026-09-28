package com.subhrodip.pennywise.observability.security

private const val BEARER_PREFIX = "Bearer "
private const val REDACTED_BEARER = "Bearer [REDACTED]"
private const val REDACTED = "[REDACTED]"

/** Redacts credentials and masks personal data before values reach logs. */
object SensitiveDataSanitizer {
    /** Masks an email local part while retaining its domain for diagnostics. */
    fun email(value: String): String {
        val separator = value.indexOf('@')
        if (separator <= 0 || separator == value.lastIndex) return REDACTED
        return "${value.first()}***${value.substring(separator)}"
    }

    /** Replaces a bearer token while preserving the authentication scheme. */
    fun token(value: String): String =
        if (value.trimStart().startsWith(BEARER_PREFIX, ignoreCase = true)) REDACTED_BEARER else REDACTED
}
