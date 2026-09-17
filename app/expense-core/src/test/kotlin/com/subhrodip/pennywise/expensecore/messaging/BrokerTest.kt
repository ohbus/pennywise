package com.subhrodip.pennywise.expensecore.messaging

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class BrokerTest {
    private val eventId = UUID.randomUUID()
    private val message = BrokerMessage(eventId, "expense.created", "{}".toByteArray(), Instant.EPOCH)

    @Test fun `duplicate event IDs are delivered once`() {
        val broker = InMemoryBroker()
        broker.publish(message)
        broker.publish(message)
        assertEquals(1, broker.size())
    }

    @Test fun `failed handling retains message for retry`() {
        val broker = InMemoryBroker()
        broker.publish(message)
        broker.receive { false }
        assertEquals(1, broker.size())
    }

    @Test fun `successful handling acknowledges message`() {
        val broker = InMemoryBroker()
        broker.publish(message)
        var delivered = false
        broker.receive { delivered = it.eventId == eventId; delivered }
        assertTrue(delivered)
        assertEquals(0, broker.size())
    }
}
