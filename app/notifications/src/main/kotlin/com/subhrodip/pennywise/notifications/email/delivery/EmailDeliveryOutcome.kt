package com.subhrodip.pennywise.notifications.email.delivery
enum class EmailDeliveryOutcome {
    DELIVERED,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
