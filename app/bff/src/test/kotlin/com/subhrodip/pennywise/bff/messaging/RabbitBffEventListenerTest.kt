package com.subhrodip.pennywise.bff.messaging

import com.rabbitmq.client.Channel
import com.subhrodip.pennywise.bff.LiveUpdateFanout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

class RabbitBffEventListenerTest {

    private val fanout = LiveUpdateFanout()
    private val deduplicator = BffEventDeduplicator()
    private val eventConsumer = BffEventConsumer(fanout, deduplicator)
    private val objectMapper = ObjectMapper()
    private val listener = RabbitBffEventListener(eventConsumer, objectMapper)
    private val channel: Channel = mock(Channel::class.java)

    @Test
    fun `acknowledges valid envelope message and triggers fanout`() {
        val eventId = UUID.randomUUID()
        val aggregateId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val subscriber = fanout.subscribe("user-1", groupId.toString())

        val json = """
            {
              "eventId": "$eventId",
              "eventType": "group.updated",
              "schemaVersion": 1,
              "aggregateId": "$aggregateId",
              "groupId": "$groupId",
              "groupRevision": 10,
              "occurredAt": "${Instant.now()}",
              "payload": {"name": "Summer Vacation"}
            }
        """.trimIndent()

        val properties = MessageProperties().apply { deliveryTag = 42L }
        val message = Message(json.toByteArray(Charsets.UTF_8), properties)

        listener.onMessage(message, channel)

        assertEquals(1, fanout.pendingCount(subscriber.id))
        val update = fanout.poll(subscriber.id)
        assertEquals(groupId.toString(), update?.groupId)
        assertEquals(10L, update?.revision)

        verify(channel).basicAck(42L, false)
        verify(channel, never()).basicReject(42L, false)
    }

    @Test
    fun `rejects malformed poison-pill message without requeue`() {
        val malformedJson = "{ not-valid-json }"
        val properties = MessageProperties().apply { deliveryTag = 99L }
        val message = Message(malformedJson.toByteArray(Charsets.UTF_8), properties)

        listener.onMessage(message, channel)

        verify(channel, never()).basicAck(99L, false)
        verify(channel).basicReject(99L, false)
    }
}
