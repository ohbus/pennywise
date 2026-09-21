package com.subhrodip.pennywise.notifications.consumer

/**
 * RabbitMQ listener that parses incoming broker envelopes and forwards them to the
 * notification consumer. Handles acknowledgement, poison‑pill rejection, and
 * transient re‑queue semantics.
 */

import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

private val log = LoggerFactory.getLogger(RabbitNotificationListener::class.java)


class InvalidEnvelopeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class BrokerEnvelope(
    val eventId: UUID,
    val eventType: String,
    val schemaVersion: Int,
    val aggregateId: UUID,
    val groupId: UUID,
    val groupRevision: Long,
    val occurredAt: Instant,
    val payload: Map<String, Any?>
)

@Component
class BrokerEnvelopeParser(
    private val objectMapper: ObjectMapper = ObjectMapper()
) {
    fun parse(bytes: ByteArray): BrokerEnvelope = try {
        val root: JsonNode = objectMapper.readTree(bytes)
            ?: throw InvalidEnvelopeException("Empty message body")

        val eventId = root.getRequiredUuid("eventId")
        val eventType = root.getRequiredString("eventType")
        val schemaVersion = root.getRequiredInt("schemaVersion").also {
            if (it < 1) throw InvalidEnvelopeException("schemaVersion must be >= 1")
        }
        val aggregateId = root.getRequiredUuid("aggregateId")
        val groupId = root.getRequiredUuid("groupId")
        val groupRevision = root.getRequiredLong("groupRevision").also {
            if (it < 1) throw InvalidEnvelopeException("groupRevision must be >= 1")
        }
        val occurredAtStr = root.getRequiredString("occurredAt")
        val occurredAt = try {
            Instant.parse(occurredAtStr)
        } catch (e: Exception) {
            throw InvalidEnvelopeException("Invalid occurredAt ISO-8601 string: $occurredAtStr", e)
        }

        val payloadNode = root.get("payload")
        if (payloadNode == null || !payloadNode.isObject) {
            throw InvalidEnvelopeException("payload must be an object")
        }

        val payloadMap = mutableMapOf<String, Any?>()
        payloadNode.properties().forEach { entry ->
            val valueNode = entry.value
            payloadMap[entry.key] = when {
                valueNode.isString -> valueNode.asString()
                valueNode.isIntegralNumber -> valueNode.asLong()
                valueNode.isFloatingPointNumber -> valueNode.asDouble()
                valueNode.isBoolean -> valueNode.asBoolean()
                valueNode.isNull -> null
                else -> valueNode.toString()
            }
        }

        BrokerEnvelope(
            eventId = eventId,
            eventType = eventType,
            schemaVersion = schemaVersion,
            aggregateId = aggregateId,
            groupId = groupId,
            groupRevision = groupRevision,
            occurredAt = occurredAt,
            payload = payloadMap
        )
    } catch (e: InvalidEnvelopeException) {
        throw e
    } catch (e: Exception) {
        throw InvalidEnvelopeException("Failed to parse broker envelope: ${e.message}", e)
    }

    private fun JsonNode.getRequiredString(field: String): String {
        val node = get(field)
        if (node == null || !node.isString || node.asString().isBlank()) {
            throw InvalidEnvelopeException("Missing or invalid string field: $field")
        }
        return node.asString()
    }

    private fun JsonNode.getRequiredUuid(field: String): UUID {
        val str = getRequiredString(field)
        return try {
            UUID.fromString(str)
        } catch (e: Exception) {
            throw InvalidEnvelopeException("Invalid UUID in field $field: $str", e)
        }
    }

    private fun JsonNode.getRequiredInt(field: String): Int {
        val node = get(field)
        if (node == null || !node.isIntegralNumber) {
            throw InvalidEnvelopeException("Missing or invalid integer field: $field")
        }
        return node.asInt()
    }

    private fun JsonNode.getRequiredLong(field: String): Long {
        val node = get(field)
        if (node == null || !node.isIntegralNumber) {
            throw InvalidEnvelopeException("Missing or invalid long field: $field")
        }
        return node.asLong()
    }
}

fun BrokerEnvelope.toNotificationEvent(): NotificationEvent {
    val notifId = (payload["notificationId"] as? String)?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    } ?: aggregateId

    val subject = (payload["subject"] as? String
        ?: payload["recipient"] as? String
        ?: payload["recipientId"] as? String
        ?: payload["userId"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: groupId.toString()

    val message = (payload["message"] as? String
        ?: payload["description"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "$eventType for group $groupId"

    return NotificationEvent(
        eventId = eventId,
        notificationId = notifId,
        subject = subject.take(200),
        eventType = eventType.take(200),
        message = message.take(2000),
        occurredAt = occurredAt
    )
}

/**
 * Transport adapter connecting RabbitMQ queues with the transactional NotificationEventConsumer.
 * Validates envelopes against the contract, acknowledges processed/duplicate messages,
 * rejects malformed/poison messages without requeue, and rejects transient failures with requeue.
 */
@Component
class RabbitNotificationListener(
    private val consumer: NotificationConsumer,
    private val envelopeParser: BrokerEnvelopeParser
) : ChannelAwareMessageListener {

    @RabbitListener(
        queues = ["\${pennywise.notifications.queue:pennywise.notifications.v2}"],
        ackMode = "MANUAL"
    )
    override fun onMessage(message: Message, channel: Channel?) {
        val deliveryTag = message.messageProperties.deliveryTag
        try {
            val envelope = envelopeParser.parse(message.body)
            val event = envelope.toNotificationEvent()
            consumer.consume(event)
            channel?.basicAck(deliveryTag, false)
        } catch (e: InvalidEnvelopeException) {
            // Poison pill: reject without requeue
            channel?.basicReject(deliveryTag, false)
        } catch (t: Throwable) {
            // Give a transient failure one retry, then park it in the DLQ rather
            // than allowing an indefinitely redelivered poison message.
            channel?.basicReject(deliveryTag, message.messageProperties.redelivered != true)
        }
    }
}
