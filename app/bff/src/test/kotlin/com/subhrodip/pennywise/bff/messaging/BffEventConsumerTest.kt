package com.subhrodip.pennywise.bff.messaging

import com.subhrodip.pennywise.bff.LiveUpdateFanout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BffEventConsumerTest {

    private lateinit var fanout: LiveUpdateFanout
    private lateinit var deduplicator: BffEventDeduplicator
    private lateinit var consumer: BffEventConsumer

    @BeforeEach
    fun setUp() {
        fanout = LiveUpdateFanout()
        deduplicator = BffEventDeduplicator(100)
        consumer = BffEventConsumer(fanout, deduplicator)
    }

    @Test
    fun `consumes event and emits invalidation and updates subscribed queues`() {
        val groupId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val sub1 = fanout.subscribe("user-1", groupId.toString())

        val envelope = BffEventEnvelope(
            eventId = eventId,
            eventType = "expense.created",
            schemaVersion = 1,
            aggregateId = UUID.randomUUID(),
            groupId = groupId,
            groupRevision = 5L,
            occurredAt = Instant.now()
        )

        val result = consumer.consume(envelope)
        assertTrue(result is ConsumptionResult.Processed)

        val processed = result as ConsumptionResult.Processed
        assertEquals(groupId.toString(), processed.invalidation.groupId)
        assertEquals(5L, processed.invalidation.revision)
        assertEquals(eventId.toString(), processed.invalidation.changeId)
        assertEquals(1, processed.deliveredQueues)

        val polled = fanout.poll(sub1.id)
        assertEquals(groupId.toString(), polled?.groupId)
        assertEquals(5L, polled?.revision)
    }

    @Test
    fun `deduplicates re-delivered events by eventId without redundant fanout`() {
        val groupId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val sub = fanout.subscribe("user-1", groupId.toString())

        val envelope = BffEventEnvelope(
            eventId = eventId,
            eventType = "expense.created",
            schemaVersion = 1,
            aggregateId = UUID.randomUUID(),
            groupId = groupId,
            groupRevision = 2L,
            occurredAt = Instant.now()
        )

        val firstResult = consumer.consume(envelope)
        assertTrue(firstResult is ConsumptionResult.Processed)
        assertEquals(1, fanout.pendingCount(sub.id))

        // Second delivery with identical eventId
        val secondResult = consumer.consume(envelope)
        assertTrue(secondResult is ConsumptionResult.Duplicate)
        val duplicate = secondResult as ConsumptionResult.Duplicate
        assertEquals(eventId, duplicate.eventId)

        // Queue must still have only 1 pending update, not 2
        assertEquals(1, fanout.pendingCount(sub.id))
    }

    @Test
    fun `deduplicator evicts oldest entry when capacity is reached`() {
        val smallDeduplicator = BffEventDeduplicator(2)
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()

        assertEquals(false, smallDeduplicator.isDuplicateAndMark(id1))
        assertEquals(false, smallDeduplicator.isDuplicateAndMark(id2))
        assertEquals(true, smallDeduplicator.isDuplicateAndMark(id1)) // access id1, making id2 eldest
        assertEquals(false, smallDeduplicator.isDuplicateAndMark(id3)) // should evict id2

        assertEquals(false, smallDeduplicator.isDuplicateAndMark(id2)) // id2 was evicted, so recognized as new
    }
}
