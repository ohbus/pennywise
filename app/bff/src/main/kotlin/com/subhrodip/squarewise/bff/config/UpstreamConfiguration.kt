@file:Suppress("CanConvertToMultiDollarString")

package com.subhrodip.squarewise.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Validates that deployed BFF profiles provide explicit downstream service addresses.
 */
@Configuration
@Profile("production", "staging", "local-oidc")
class UpstreamConfiguration(
    @Value("\${squarewise.accounts-url:}") accountsUrl: String,
    @Value("\${squarewise.expense-core-url:}") expenseCoreUrl: String
) {
    init {
        require(accountsUrl.isNotBlank()) {
            "squarewise.accounts-url is required in deployed BFF profiles"
        }
        require(expenseCoreUrl.isNotBlank()) {
            "squarewise.expense-core-url is required in deployed BFF profiles"
        }
    }
}
