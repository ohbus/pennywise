package com.subhrodip.squarewise.bff.messaging.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies deployed BFF profiles fail closed when RabbitMQ fanout is disabled. */
class RequiredBffMessagingConfigurationTest {
    /** Verifies enabled messaging configuration is accepted during startup validation. */
    @Test
    fun `accepts enabled messaging`() {
        assertDoesNotThrow {
            RequiredBffMessagingConfiguration(BffMessagingProperties(enabled = true))
        }
    }

    /** Verifies disabled messaging configuration cannot silently start a deployed profile. */
    @Test
    fun `rejects disabled messaging`() {
        assertThrows(IllegalArgumentException::class.java) {
            RequiredBffMessagingConfiguration(BffMessagingProperties(enabled = false))
        }
    }
}
