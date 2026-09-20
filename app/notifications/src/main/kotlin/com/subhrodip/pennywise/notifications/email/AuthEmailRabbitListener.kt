package com.subhrodip.pennywise.notifications.email

import com.rabbitmq.client.Channel
import com.subhrodip.pennywise.notifications.consumer.InvalidEnvelopeException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import org.springframework.stereotype.Component

private const val AUTH_EMAIL_EVENT_TYPE = "auth.email.requested.v1"

/** Consumes encrypted auth-email envelopes and delegates plaintext handling only inside Notifications. */
@Component
class AuthEmailRabbitListener(
    private val objectMapper: ObjectMapper,
    private val consumer: AuthEmailDeliveryConsumer
) : ChannelAwareMessageListener {
    @RabbitListener(
        queues = ["\${pennywise.notifications.auth-email-queue:pennywise.auth-email}"],
        ackMode = "MANUAL"
    )
    override fun onMessage(message: Message, channel: Channel?) {
        val deliveryTag = message.messageProperties.deliveryTag
        try {
            val event = parse(message.body)
            consumer.consume(event)
            channel?.basicAck(deliveryTag, false)
        } catch (_: InvalidEnvelopeException) {
            channel?.basicReject(deliveryTag, false)
        } catch (error: IllegalArgumentException) {
            channel?.basicReject(deliveryTag, false)
        } catch (_: Throwable) {
            channel?.basicReject(deliveryTag, true)
        }
    }

    private fun parse(body: ByteArray): AuthEmailDeliveryEvent {
        val root = objectMapper.readTree(body)
            ?: throw InvalidEnvelopeException("Empty auth email event")
        val eventType = required(root, "eventType").asString()
        if (eventType != AUTH_EMAIL_EVENT_TYPE) {
            throw InvalidEnvelopeException("Unexpected auth email event type")
        }
        val payload = root.get("payload")
            ?: throw InvalidEnvelopeException("Auth email payload is missing")
        return AuthEmailDeliveryEvent(
            eventId = required(root, "eventId").asString(),
            recipient = required(payload, "recipient").asString(),
            template = required(payload, "template").asString(),
            encryptedCredential = required(payload, "encryptedCredential").asString(),
            expiresAt = runCatching { Instant.parse(required(payload, "expiresAt").asString()) }
                .getOrElse { throw InvalidEnvelopeException("Invalid auth email expiry", it) }
        )
    }

    private fun required(node: JsonNode, field: String): JsonNode =
        node.get(field)?.takeIf { !it.isNull } ?: throw InvalidEnvelopeException("Missing auth email field: $field")
}
