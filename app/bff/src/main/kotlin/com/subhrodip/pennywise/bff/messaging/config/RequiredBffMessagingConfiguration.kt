package com.subhrodip.pennywise.bff.messaging.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Prevents deployed and local OIDC BFF instances from starting without RabbitMQ fanout. */
@Configuration
@Profile("production", "staging", "local-oidc")
class RequiredBffMessagingConfiguration(properties: BffMessagingProperties) {
    init {
        require(properties.enabled) {
            "pennywise.bff.messaging.enabled must be true in deployed and local-oidc profiles"
        }
    }
}
