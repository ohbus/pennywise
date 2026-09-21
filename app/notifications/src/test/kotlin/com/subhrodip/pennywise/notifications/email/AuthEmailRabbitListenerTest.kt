package com.subhrodip.pennywise.notifications.email

import com.rabbitmq.client.Channel
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.any
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import tools.jackson.databind.ObjectMapper

class AuthEmailRabbitListenerTest {
    private val consumer = mock(AuthEmailDeliveryConsumer::class.java)
    private val listener = AuthEmailRabbitListener(ObjectMapper(), consumer)

    @Test
    fun `rejects malformed auth email without requeue`() {
        val channel = TestChannel()
        listener.onMessage(message("{not-json}", 11L), channel)

        assertEquals(11L, channel.rejectedTag)
        assertEquals(false, channel.rejectedRequeue)
        assertNull(channel.ackedTag)
    }

    @Test
    fun `rejects redelivered transient auth email failure without requeue`() {
        val event = """{"eventType":"auth.email.requested.v1","eventId":"evt-1","payload":{"recipient":"user@example.com","template":"LOGIN_CODE","encryptedCredential":"cipher","expiresAt":"2026-09-21T01:00:00Z"}}"""
        doThrow(IllegalStateException("temporary failure"))
            .`when`(consumer)
            .consume(
                any(AuthEmailDeliveryEvent::class.java) ?: AuthEmailDeliveryEvent("evt", "user@example.com", "LOGIN_CODE", "cipher", java.time.Instant.MAX),
                any(java.time.Instant::class.java) ?: java.time.Instant.EPOCH
            )
        val channel = TestChannel()

        listener.onMessage(message(event, 12L, redelivered = true), channel)

        assertEquals(12L, channel.rejectedTag)
        assertEquals(false, channel.rejectedRequeue)
    }

    private fun message(body: String, tag: Long, redelivered: Boolean = false): Message = Message(
        body.toByteArray(StandardCharsets.UTF_8),
        MessageProperties().apply {
            deliveryTag = tag
            isRedelivered = redelivered
        }
    )

    private class TestChannel : Channel by mock(Channel::class.java) {
        var ackedTag: Long? = null
        var rejectedTag: Long? = null
        var rejectedRequeue: Boolean? = null

        override fun basicAck(deliveryTag: Long, multiple: Boolean) { ackedTag = deliveryTag }
        override fun basicReject(deliveryTag: Long, requeue: Boolean) {
            rejectedTag = deliveryTag
            rejectedRequeue = requeue
        }
    }
}
