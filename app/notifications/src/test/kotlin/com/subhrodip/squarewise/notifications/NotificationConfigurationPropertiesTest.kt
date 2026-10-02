package com.subhrodip.squarewise.notifications

import com.subhrodip.squarewise.notifications.consumer.config.NotificationMessagingProperties
import com.subhrodip.squarewise.notifications.email.config.EmailProperties
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies notification broker and SMTP properties retain values supplied by binding. */
class NotificationConfigurationPropertiesTest {
    @Test
    fun `notification messaging properties expose safe queue defaults`() {
        val properties = NotificationMessagingProperties()

        assertEquals("squarewise.notifications.v2", properties.queue)
        assertEquals("squarewise.auth-email.v2", properties.authEmailQueue)
        assertEquals("squarewise.events.dlx", properties.deadLetterExchange)
        assertEquals("squarewise.notifications.v2.dlq", properties.deadLetterQueue)
        assertEquals("squarewise.auth-email.v2.dlq", properties.authEmailDeadLetterQueue)
    }

    @Test
    fun `notification properties accept bound SMTP and queue values`() {
        val email = EmailProperties(
            host = "smtp.example.test",
            port = 2525,
            fromAddress = "notifications@example.test",
            enabled = false,
            maxAttempts = 4,
            retryDelayMs = 250
        )
        email.host = "mail.example.test"
        email.port = 1025
        email.fromAddress = "noreply@example.test"
        email.enabled = true
        email.maxAttempts = 2
        email.retryDelayMs = 100

        val messaging = NotificationMessagingProperties()
        messaging.queue = "notifications.test"
        messaging.authEmailQueue = "auth-email.test"
        messaging.deadLetterExchange = "events.test.dlx"
        messaging.deadLetterQueue = "notifications.test.dlq"
        messaging.authEmailDeadLetterQueue = "auth-email.test.dlq"

        assertEquals("mail.example.test", email.host)
        assertEquals(1025, email.port)
        assertEquals("noreply@example.test", email.fromAddress)
        assertEquals(true, email.enabled)
        assertEquals(2, email.maxAttempts)
        assertEquals(100, email.retryDelayMs)
        assertEquals("notifications.test", messaging.queue)
        assertEquals("auth-email.test", messaging.authEmailQueue)
        assertEquals("events.test.dlx", messaging.deadLetterExchange)
        assertEquals("notifications.test.dlq", messaging.deadLetterQueue)
        assertEquals("auth-email.test.dlq", messaging.authEmailDeadLetterQueue)
    }
}
