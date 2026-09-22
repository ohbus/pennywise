package com.subhrodip.pennywise.expensecore.sync.domain

/** Raised when a cursor is malformed, expired, or belongs to another group. */
class InvalidSyncCursorException : RuntimeException("The synchronization cursor is invalid or expired")
