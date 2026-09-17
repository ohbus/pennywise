package com.subhrodip.pennywise.notifications.email

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "pennywise.notifications.email")
data class EmailProperties(
    var host: String = "localhost",
    var port: Int = 1025,
    var fromAddress: String = "notifications@pennywise.local",
    var enabled: Boolean = true,
    var maxAttempts: Int = 3,
    var retryDelayMs: Long = 0L
)
