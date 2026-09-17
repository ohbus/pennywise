package com.subhrodip.pennywise.notifications.email

enum class EmailDeliveryOutcome {
    DELIVERED,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
