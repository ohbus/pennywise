package com.subhrodip.pennywise.expensecore.messaging
import com.subhrodip.pennywise.expensecore.messaging.broker.BrokerMessage
import com.subhrodip.pennywise.expensecore.messaging.broker.BrokerPublisher
import com.subhrodip.pennywise.expensecore.messaging.broker.PublishResult
import com.subhrodip.pennywise.expensecore.messaging.outbox.model.OutboxMessage
import com.subhrodip.pennywise.expensecore.messaging.outbox.service.OutboxPublisher
import com.subhrodip.pennywise.expensecore.messaging.outbox.service.OutboxRelay
import com.subhrodip.pennywise.expensecore.messaging.outbox.model.OutboxStatus
import com.subhrodip.pennywise.expensecore.messaging.outbox.model.PublishBatchResult
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class OutboxPublisherTest {
    @Test
    fun `confirm publishes and acknowledges with stable metadata`() {
        val relay = OutboxRelay { Instant.EPOCH }
        val eventId = UUID.randomUUID()
        relay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 4, Instant.EPOCH, mapOf("b" to 2, "a" to 1)))
        var received: BrokerMessage? = null
        val publisher = OutboxPublisher(relay, BrokerPublisher { message -> received = message; PublishResult.Confirmed }, ObjectMapper())

        assertEquals(PublishBatchResult(1, 1, 0), publisher.publishAvailable())
        assertEquals(OutboxStatus.PUBLISHED, relay.snapshot().single().status)
        val payloadString = received?.payload?.toString(Charsets.UTF_8)
        assertTrue(payloadString == "{\"a\":1,\"b\":2}" || payloadString == "{\"b\":2,\"a\":1}")
        assertEquals("4", received?.headers?.get("group-revision"))
    }

    @Test
    fun `rejected publish remains pending until attempts are exhausted`() {
        val relay = OutboxRelay { Instant.EPOCH }
        val eventId = UUID.randomUUID()
        relay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 1, Instant.EPOCH, emptyMap()))
        val publisher = OutboxPublisher(relay, BrokerPublisher { PublishResult.Rejected("broker unavailable") }, ObjectMapper(), maxAttempts = 2)

        assertEquals(PublishBatchResult(1, 0, 1), publisher.publishAvailable())
        assertEquals(OutboxStatus.PENDING, relay.snapshot().single().status)
        val secondRelay = OutboxRelay { Instant.EPOCH }
        secondRelay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 1, Instant.EPOCH, emptyMap(), attempts = 1))
        assertEquals(PublishBatchResult(1, 0, 1), OutboxPublisher(secondRelay, BrokerPublisher { PublishResult.Rejected("down") }, ObjectMapper(), maxAttempts = 2).publishAvailable())
        assertTrue(secondRelay.snapshot().single().status == OutboxStatus.PARKED)
    }
}
