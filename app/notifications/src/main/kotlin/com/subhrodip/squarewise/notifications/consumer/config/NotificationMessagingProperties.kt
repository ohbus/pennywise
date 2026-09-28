package com.subhrodip.squarewise.notifications.consumer.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration properties for notification and authentication-email broker resources. */
@ConfigurationProperties(prefix = "squarewise.notifications")
data class NotificationMessagingProperties(
    var queue: String = "squarewise.notifications.v2",
    var authEmailQueue: String = "squarewise.auth-email.v2",
    var deadLetterExchange: String = "squarewise.events.dlx",
    var deadLetterQueue: String = "squarewise.notifications.v2.dlq",
    var authEmailDeadLetterQueue: String = "squarewise.auth-email.v2.dlq"
)
