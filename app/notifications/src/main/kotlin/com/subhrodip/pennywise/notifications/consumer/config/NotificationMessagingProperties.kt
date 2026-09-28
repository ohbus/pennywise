package com.subhrodip.pennywise.notifications.consumer.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration properties for notification and authentication-email broker resources. */
@ConfigurationProperties(prefix = "pennywise.notifications")
data class NotificationMessagingProperties(
    var queue: String = "pennywise.notifications.v2",
    var authEmailQueue: String = "pennywise.auth-email.v2",
    var deadLetterExchange: String = "pennywise.events.dlx",
    var deadLetterQueue: String = "pennywise.notifications.v2.dlq",
    var authEmailDeadLetterQueue: String = "pennywise.auth-email.v2.dlq"
)
