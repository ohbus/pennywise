package com.subhrodip.pennywise.errors.http

import java.time.Instant

/** Problem-detail response exposed by the service error boundary. */
data class ApiProblem(
    val type: String = "https://pennywise.example/problems",
    val title: String,
    val status: Int,
    val code: String? = null,
    /** Service or bounded-context identifier that generated this problem. */
    val source: String = "unknown",
    val requestId: String,
    val detail: String,
    val timestamp: Instant = Instant.now(),
    val violations: List<FieldViolation> = emptyList()
)
