package com.subhrodip.pennywise.bff

import com.subhrodip.pennywise.bff.transport.ExpenseCoreGateway
import com.subhrodip.pennywise.bff.transport.AccountsGateway
import com.subhrodip.pennywise.bff.messaging.model.BffEventEnvelope
import com.subhrodip.pennywise.bff.messaging.model.ConsumptionResult
import com.subhrodip.pennywise.bff.messaging.model.DuplicateConsumptionResult
import com.subhrodip.pennywise.bff.messaging.model.ProcessedConsumptionResult
import com.subhrodip.pennywise.bff.messaging.service.BffEventConsumer
import com.subhrodip.pennywise.bff.messaging.persistence.BffEventDeduplicator
import com.subhrodip.pennywise.bff.realtime.GroupInvalidation
import com.subhrodip.pennywise.bff.realtime.LiveUpdate
import com.subhrodip.pennywise.bff.realtime.LiveUpdateFanout

import com.subhrodip.pennywise.bff.config.UpstreamConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UpstreamConfigurationTest {
    @Test
    fun `rejects missing deployed downstream addresses`() {
        assertThrows<IllegalArgumentException> { UpstreamConfiguration("", "http://expense-core") }
        assertThrows<IllegalArgumentException> { UpstreamConfiguration("http://accounts", "") }
    }

    @Test
    fun `accepts explicit deployed downstream addresses`() {
        assertDoesNotThrow { UpstreamConfiguration("http://accounts", "http://expense-core") }
    }
}
