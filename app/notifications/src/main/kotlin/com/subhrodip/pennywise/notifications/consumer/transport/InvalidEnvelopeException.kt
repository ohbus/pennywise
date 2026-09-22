package com.subhrodip.pennywise.notifications.consumer.transport

/** Raised when a broker envelope violates the notification event contract. */
class InvalidEnvelopeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
