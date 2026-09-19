package com.subhrodip.pennywise.expensecore.messaging

import java.time.Duration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "pennywise.outbox")
data class OutboxRelayProperties(
    var enabled: Boolean = false,
    var batchSize: Int = 100,
    var pollDelayMs: Long = 1000,
    var leaseSeconds: Long = 30,
    var maxAttempts: Int = 5,
    var retryAfterSeconds: Long = 5
)

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxRelayProperties::class)
class OutboxMessagingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "pennywise.outbox", name = ["rabbit-enabled"], havingValue = "true")
    fun rabbitBrokerPublisher(
        rabbitTemplate: org.springframework.amqp.rabbit.core.RabbitTemplate,
        objectMapper: tools.jackson.databind.ObjectMapper
    ): BrokerPublisher = RabbitBrokerPublisher(rabbitTemplate, objectMapper)

    @Bean
    @ConditionalOnMissingBean(BrokerPublisher::class)
    fun defaultBrokerPublisher(): BrokerPublisher = InMemoryBroker()

    @Bean
    @ConditionalOnMissingBean(OutboxPublisher::class)
    fun outboxPublisher(
        relay: OutboxStore,
        brokerPublisher: BrokerPublisher,
        objectMapper: org.springframework.beans.factory.ObjectProvider<tools.jackson.databind.ObjectMapper>,
        properties: OutboxRelayProperties
    ): OutboxPublisher = OutboxPublisher(
        relay = relay,
        publisher = brokerPublisher,
        objectMapper = objectMapper.ifAvailable ?: tools.jackson.databind.ObjectMapper(),
        batchSize = properties.batchSize,
        lease = Duration.ofSeconds(properties.leaseSeconds),
        maxAttempts = properties.maxAttempts,
        retryAfter = Duration.ofSeconds(properties.retryAfterSeconds)
    )
}

/**
 * Background daemon polling available outbox messages and pumping them to the broker.
 * Gated by `pennywise.outbox.enabled=true` so tests and non-worker containers remain isolated.
 */
@Component
@ConditionalOnProperty(prefix = "pennywise.outbox", name = ["enabled"], havingValue = "true")
class OutboxRelayDaemon(
    private val publisher: OutboxPublisher
) {
    @Scheduled(fixedDelayString = "\${pennywise.outbox.poll-delay-ms:1000}")
    fun pollAndPublish(): PublishBatchResult {
        return publisher.publishAvailable()
    }
}
