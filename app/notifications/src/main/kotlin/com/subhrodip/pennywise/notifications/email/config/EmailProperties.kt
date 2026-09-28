package com.subhrodip.pennywise.notifications.email.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pennywise.notifications.email")
data class EmailProperties(
    var host: String,
    var port: Int,
    var fromAddress: String,
    var enabled: Boolean,
    var maxAttempts: Int,
    var retryDelayMs: Long
)
