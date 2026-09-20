package com.subhrodip.pennywise.accounts.auth.delivery

import java.time.Duration
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/** Configuration for the Accounts-owned protected auth-email outbox publisher. */
@ConfigurationProperties(prefix = "pennywise.auth-email-outbox")
data class AuthEmailOutboxProperties(
    var enabled: Boolean = false,
    var exchange: String = "pennywise.events",
    var routingKey: String = "auth.email.requested.v1",
    var pollDelayMs: Long = 1000,
    var leaseSeconds: Long = 30,
    var retryAfterSeconds: Long = 5,
    var maximumAttempts: Int = 5
)

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AuthEmailOutboxProperties::class)
class AuthEmailMessagingConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "pennywise.auth-email-outbox", name = ["enabled"], havingValue = "true")
    fun authEmailOutboxPublisher(outbox: AuthEmailOutboxService, rabbitTemplate: RabbitTemplate, objectMapper: ObjectMapper, properties: AuthEmailOutboxProperties): AuthEmailOutboxPublisher =
        AuthEmailOutboxPublisher(outbox, rabbitTemplate, objectMapper, properties.exchange, properties.routingKey, Duration.ofSeconds(properties.leaseSeconds), Duration.ofSeconds(properties.retryAfterSeconds), properties.maximumAttempts)
}

/** Polls the protected auth-email outbox only when explicitly enabled. */
@Component
@ConditionalOnProperty(prefix = "pennywise.auth-email-outbox", name = ["enabled"], havingValue = "true")
class AuthEmailOutboxRelay(private val publisher: AuthEmailOutboxPublisher) {
    @Scheduled(fixedDelayString = "\${pennywise.auth-email-outbox.poll-delay-ms:1000}")
    fun poll(): AuthEmailPublishOutcome = publisher.publishOne()
}
