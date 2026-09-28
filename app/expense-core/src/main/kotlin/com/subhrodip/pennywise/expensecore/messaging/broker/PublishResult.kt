package com.subhrodip.pennywise.expensecore.messaging.broker
/** Outcome returned by a broker publisher. */
sealed interface PublishResult {
    /** Broker accepted the message. */
    data object Confirmed : PublishResult
    /** Broker rejected the message with a safe reason. */
    data class Rejected(val reason: String) : PublishResult
}
