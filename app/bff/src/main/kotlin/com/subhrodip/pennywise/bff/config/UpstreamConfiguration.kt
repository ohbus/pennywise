package com.subhrodip.pennywise.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Validates that deployed BFF profiles provide explicit downstream service addresses.
 */
@Configuration
@Profile("production", "staging", "local-oidc")
class UpstreamConfiguration(
    @Value("\${pennywise.accounts-url:}") accountsUrl: String,
    @Value("\${pennywise.expense-core-url:}") expenseCoreUrl: String
) {
    init {
        require(accountsUrl.isNotBlank()) {
            "pennywise.accounts-url is required in deployed BFF profiles"
        }
        require(expenseCoreUrl.isNotBlank()) {
            "pennywise.expense-core-url is required in deployed BFF profiles"
        }
    }
}
