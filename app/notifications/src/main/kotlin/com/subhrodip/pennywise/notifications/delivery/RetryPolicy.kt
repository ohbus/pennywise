package com.subhrodip.pennywise.notifications.delivery

import java.time.Duration

enum class DeliveryOutcome { RETRYABLE_FAILURE, PERMANENT_FAILURE, SUCCESS }
data class RetryDecision(val retry: Boolean, val parked: Boolean, val delay: Duration)

class RetryPolicy(private val maxAttempts: Int = 5) {
    init { require(maxAttempts > 0) }
    fun decide(attempt: Int, outcome: DeliveryOutcome): RetryDecision {
        require(attempt > 0)
        return when (outcome) {
            DeliveryOutcome.SUCCESS -> RetryDecision(false, false, Duration.ZERO)
            DeliveryOutcome.PERMANENT_FAILURE -> RetryDecision(false, true, Duration.ZERO)
            DeliveryOutcome.RETRYABLE_FAILURE -> if (attempt >= maxAttempts) RetryDecision(false, true, Duration.ZERO)
            else RetryDecision(true, false, Duration.ofSeconds(1L shl (attempt - 1).coerceAtMost(10)))
        }
    }
}
