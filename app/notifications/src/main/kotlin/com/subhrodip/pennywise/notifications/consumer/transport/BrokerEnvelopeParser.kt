package com.subhrodip.pennywise.notifications.consumer.transport

import java.time.Instant
import java.util.UUID
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/** Parses and validates broker envelopes before notification delivery. */
class BrokerEnvelopeParser(private val objectMapper: ObjectMapper) {
    fun parse(bytes: ByteArray): BrokerEnvelope = try {
        val root: JsonNode = objectMapper.readTree(bytes) ?: throw InvalidEnvelopeException("Empty message body")
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
        val occurredAtString = root.getRequiredString("occurredAt")
        val occurredAt = try {
            Instant.parse(occurredAtString)
        } catch (exception: Exception) {
            throw InvalidEnvelopeException("Invalid occurredAt ISO-8601 string: $occurredAtString", exception)
        }
        val payloadNode = root.get("payload")
        if (payloadNode == null || !payloadNode.isObject) throw InvalidEnvelopeException("payload must be an object")
        val payload = mutableMapOf<String, Any?>()
        payloadNode.properties().forEach { entry ->
            val value = entry.value
            payload[entry.key] = when {
                value.isString -> value.asString()
                value.isIntegralNumber -> value.asLong()
                value.isFloatingPointNumber -> value.asDouble()
                value.isBoolean -> value.asBoolean()
                value.isNull -> null
                else -> value.toString()
            }
        }
        BrokerEnvelope(eventId, eventType, schemaVersion, aggregateId, groupId, groupRevision, occurredAt, payload)
    } catch (exception: InvalidEnvelopeException) {
        throw exception
    } catch (exception: Exception) {
        throw InvalidEnvelopeException("Failed to parse broker envelope: ${exception.message}", exception)
    }

    private fun JsonNode.getRequiredString(field: String): String {
        val node = get(field)
        if (node == null || !node.isString || node.asString().isBlank()) {
            throw InvalidEnvelopeException("Missing or invalid string field: $field")
        }
        return node.asString()
    }

    private fun JsonNode.getRequiredUuid(field: String): UUID = try {
        UUID.fromString(getRequiredString(field))
    } catch (exception: InvalidEnvelopeException) {
        throw exception
    } catch (exception: Exception) {
        throw InvalidEnvelopeException("Invalid UUID in field $field", exception)
    }

    private fun JsonNode.getRequiredInt(field: String): Int = get(field)?.takeIf { it.isIntegralNumber }?.asInt()
        ?: throw InvalidEnvelopeException("Missing or invalid integer field: $field")

    private fun JsonNode.getRequiredLong(field: String): Long = get(field)?.takeIf { it.isIntegralNumber }?.asLong()
        ?: throw InvalidEnvelopeException("Missing or invalid long field: $field")
}
