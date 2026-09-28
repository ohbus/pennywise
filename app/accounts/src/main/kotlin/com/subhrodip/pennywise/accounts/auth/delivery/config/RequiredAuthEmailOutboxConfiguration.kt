package com.subhrodip.pennywise.accounts.auth.delivery.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Fails deployed profiles closed when passwordless email publishing is disabled. */
@Configuration
@Profile("production", "staging", "local-oidc")
class RequiredAuthEmailOutboxConfiguration(properties: AuthEmailOutboxProperties) {
    init {
        require(properties.enabled) {
            "pennywise.auth-email-outbox.enabled must be true in deployed profiles"
        }
    }
}
