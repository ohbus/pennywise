package com.subhrodip.pennywise.accounts.auth.delivery

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RequiredAuthEmailOutboxConfigurationTest {
    @Test
    fun `deployed auth email outbox requires explicit enablement`() {
        assertThrows(IllegalArgumentException::class.java) {
            RequiredAuthEmailOutboxConfiguration(AuthEmailOutboxProperties())
        }
        RequiredAuthEmailOutboxConfiguration(AuthEmailOutboxProperties(enabled = true))
    }
}
