package com.subhrodip.pennywise.errors.http

/** Identifies a request field that failed validation. */
data class FieldViolation(val field: String, val message: String)
