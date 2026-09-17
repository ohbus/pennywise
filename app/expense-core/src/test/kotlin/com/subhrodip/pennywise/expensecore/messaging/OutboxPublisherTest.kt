package com.subhrodip.pennywise.expensecore.messaging

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutboxPublisherTest {
    @Test
    fun `confirm publishes and acknowledges with stable metadata`() {
        val relay = OutboxRelay { Instant.EPOCH }
        val eventId = UUID.randomUUID()
        relay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 4, Instant.EPOCH, mapOf("b" to 2, "a" to 1)))
        var received: BrokerMessage? = null
        val publisher = OutboxPublisher(relay, BrokerPublisher { message -> received = message; PublishResult.Confirmed })

        assertEquals(PublishBatchResult(1, 1, 0), publisher.publishAvailable())
        assertEquals(OutboxStatus.PUBLISHED, relay.snapshot().single().status)
        assertEquals(eventId, received?.eventId)
        assertEquals("{\"a\":1, \"b\":2}", received?.payload?.toString(Charsets.UTF_8))
        assertEquals("4", received?.headers?.get("group-revision"))
    }

    @Test
    fun `rejected publish remains pending until attempts are exhausted`() {
        val relay = OutboxRelay { Instant.EPOCH }
        val eventId = UUID.randomUUID()
        relay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 1, Instant.EPOCH, emptyMap()))
        val publisher = OutboxPublisher(relay, BrokerPublisher { PublishResult.Rejected("broker unavailable") }, maxAttempts = 2)

        assertEquals(PublishBatchResult(1, 0, 1), publisher.publishAvailable())
        assertEquals(OutboxStatus.PENDING, relay.snapshot().single().status)
        val secondRelay = OutboxRelay { Instant.EPOCH }
        secondRelay.append(OutboxMessage(eventId, "expense.created", UUID.randomUUID(), UUID.randomUUID(), 1, Instant.EPOCH, emptyMap(), attempts = 1))
        assertEquals(PublishBatchResult(1, 0, 1), OutboxPublisher(secondRelay, BrokerPublisher { PublishResult.Rejected("down") }, maxAttempts = 2).publishAvailable())
        assertTrue(secondRelay.snapshot().single().status == OutboxStatus.PARKED)
    }
}
