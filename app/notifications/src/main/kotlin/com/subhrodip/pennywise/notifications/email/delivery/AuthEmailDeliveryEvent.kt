package com.subhrodip.pennywise.notifications.email.delivery
import java.time.Instant

/** Broker-decoded protected authentication-email event. */
data class AuthEmailDeliveryEvent(
    val eventId: String,
    val recipient: String,
    val template: String,
    val encryptedCredential: String,
    val expiresAt: Instant
)
