package com.subhrodip.pennywise.expensecore.messaging.config
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Fails deployed profiles closed when durable event publishing is disabled. */
@Configuration
@Profile("production", "staging", "local-oidc")
class RequiredOutboxConfiguration(properties: OutboxRelayProperties) {
    init {
        require(properties.enabled) { "pennywise.outbox.enabled must be true in deployed profiles" }
        require(properties.rabbitEnabled) { "pennywise.outbox.rabbit-enabled must be true in deployed profiles" }
    }
}
