package com.subhrodip.pennywise.errors

import java.time.Instant

enum class ErrorCode {
    VALIDATION_FAILED,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    IDEMPOTENCY_CONFLICT,
    RATE_LIMITED,
    INTERNAL_ERROR
}

data class FieldViolation(val field: String, val message: String)

data class ApiProblem(
    val type: String = "https://pennywise.example/problems",
    val title: String,
    val status: Int,
    val code: ErrorCode,
    val requestId: String,
    val detail: String,
    val timestamp: Instant = Instant.now(),
    val violations: List<FieldViolation> = emptyList()
)
