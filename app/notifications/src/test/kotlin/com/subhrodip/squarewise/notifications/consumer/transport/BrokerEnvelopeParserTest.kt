package com.subhrodip.squarewise.notifications.consumer.transport

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

/** Verifies broker-envelope validation and lossless scalar payload conversion. */
class BrokerEnvelopeParserTest {
    private val parser = BrokerEnvelopeParser(ObjectMapper())

    @Test
    fun `parses required metadata and every supported payload scalar`() {
        val eventId = UUID.fromString("00000000-0000-7000-8000-000000000301")
        val aggregateId = UUID.fromString("00000000-0000-7000-8000-000000000302")
        val groupId = UUID.fromString("00000000-0000-7000-8000-000000000303")

        val envelope = parser.parse(
            envelopeJson(eventId, aggregateId, groupId).replace(
                "\"payload\": {}",
                "\"payload\": {\"text\": \"value\", \"whole\": 4, \"fraction\": 2.5, \"enabled\": true, \"missing\": null, \"nested\": {\"key\": \"value\"}}"
            ).toByteArray()
        )

        assertEquals(eventId, envelope.eventId)
        assertEquals("expense.created", envelope.eventType)
        assertEquals(1, envelope.schemaVersion)
        assertEquals(3L, envelope.groupRevision)
        assertEquals(Instant.parse("2026-09-17T20:00:00Z"), envelope.occurredAt)
        assertEquals("value", envelope.payload["text"])
        assertEquals(4L, envelope.payload["whole"])
        assertEquals(2.5, envelope.payload["fraction"])
        assertEquals(true, envelope.payload["enabled"])
        assertEquals(null, envelope.payload["missing"])
        assertTrue(envelope.payload["nested"].toString().contains("key"))
    }

    @Test
    fun `rejects missing or blank required fields`() {
        val valid = envelopeJson(
            UUID.fromString("00000000-0000-7000-8000-000000000304"),
            UUID.fromString("00000000-0000-7000-8000-000000000305"),
            UUID.fromString("00000000-0000-7000-8000-000000000306")
        )

        listOf(
            valid.replace("\"eventType\": \"expense.created\"", "\"eventType\": \" \"") to "eventType",
            valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 0") to "schemaVersion",
            valid.replace("\"groupRevision\": 3", "\"groupRevision\": 0") to "groupRevision",
            valid.replace("\"groupId\":", "\"groupId\": \"not-a-uuid\", \"unused\":") to "groupId",
        ).forEach { (json, field) ->
            val exception = assertThrows(InvalidEnvelopeException::class.java) { parser.parse(json.toByteArray()) }
            assertTrue(exception.message.orEmpty().contains(field), "Error should identify $field")
        }
    }

    @Test
    fun `rejects malformed timestamps payloads and message bodies`() {
        val valid = envelopeJson(
            UUID.fromString("00000000-0000-7000-8000-000000000307"),
            UUID.fromString("00000000-0000-7000-8000-000000000308"),
            UUID.fromString("00000000-0000-7000-8000-000000000309")
        )

        listOf(
            "{not-json}" to "Failed to parse",
            "" to "Failed to parse",
            valid.replace("2026-09-17T20:00:00Z", "not-a-time") to "occurredAt",
            valid.replace("\"payload\": {}", "\"payload\": []") to "payload",
        ).forEach { (body, expected) ->
            val exception = assertThrows(InvalidEnvelopeException::class.java) { parser.parse(body.toByteArray()) }
            assertTrue(exception.message.orEmpty().contains(expected), "Error should identify $expected")
        }
    }

    private fun envelopeJson(eventId: UUID, aggregateId: UUID, groupId: UUID): String =
        """
        {
          "eventId": "$eventId",
          "eventType": "expense.created",
          "schemaVersion": 1,
          "aggregateId": "$aggregateId",
          "groupId": "$groupId",
          "groupRevision": 3,
          "occurredAt": "2026-09-17T20:00:00Z",
          "payload": {}
        }
        """.trimIndent()
}
