package com.subhrodip.pennywise.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Validates downstream service addresses for deployed BFF profiles.
 * Local development keeps the explicit localhost defaults in the gateway;
 * staging and production must provide target-specific addresses.
 */
@Configuration
@Profile("production", "staging")
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
