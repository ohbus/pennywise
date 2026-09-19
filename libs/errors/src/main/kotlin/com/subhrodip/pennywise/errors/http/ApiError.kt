package com.subhrodip.pennywise.errors

import java.time.Instant

data class FieldViolation(val field: String, val message: String)

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
