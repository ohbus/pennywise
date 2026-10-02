package com.subhrodip.squarewise.notifications.consumer.transport

import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
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
            valid.replace("\"eventType\": \"expense.created\",", "") to "eventType",
            valid.replace("\"eventType\": \"expense.created\"", "\"eventType\": \" \"") to "eventType",
            valid.replace("\"eventType\": \"expense.created\"", "\"eventType\": 42") to "eventType",
            valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 0") to "schemaVersion",
            valid.replace("\"groupRevision\": 3", "\"groupRevision\": 0") to "groupRevision",
            valid.replace("\"groupId\":", "\"groupId\": \"not-a-uuid\", \"unused\":") to "groupId",
        ).forEach { (json, field) ->
            val exception = assertThrows(InvalidEnvelopeException::class.java) { parser.parse(json.toByteArray()) }
            assertTrue(exception.message.orEmpty().contains(field), "Error should identify $field")
        }
    }

    /** Verifies required numeric metadata rejects absent, text, and fractional values. */
    @Test
    fun `rejects invalid required numeric metadata`() {
        val valid = envelopeJson(
            UUID.fromString("00000000-0000-7000-8000-00000000030a"),
            UUID.fromString("00000000-0000-7000-8000-00000000030b"),
            UUID.fromString("00000000-0000-7000-8000-00000000030c")
        )

        listOf(
            valid.replace("\"schemaVersion\": 1,", "") to "schemaVersion",
            valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": \"one\"") to "schemaVersion",
            valid.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1.5") to "schemaVersion",
            valid.replace("\"groupRevision\": 3,", "") to "groupRevision",
            valid.replace("\"groupRevision\": 3", "\"groupRevision\": \"three\"") to "groupRevision",
            valid.replace("\"groupRevision\": 3", "\"groupRevision\": 3.5") to "groupRevision",
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
            "{not-json}" to null,
            "" to null,
            valid.replace("2026-09-17T20:00:00Z", "not-a-time") to "occurredAt",
            valid.replace("\"payload\": {}", "\"unrelated\": true") to "payload",
            valid.replace("\"payload\": {}", "\"payload\": []") to "payload",
        ).forEach { (body, expected) ->
            val exception = assertThrows(InvalidEnvelopeException::class.java) { parser.parse(body.toByteArray()) }
            expected?.let { message ->
                assertTrue(exception.message.orEmpty().contains(message), "Error should identify $message")
            }
        }
    }

    @Test
    fun `rejects a null parser result as an empty envelope`() {
        val objectMapper = mock(ObjectMapper::class.java)
        doReturn(null).`when`(objectMapper).readTree(any<ByteArray>())

        assertThrows(InvalidEnvelopeException::class.java) {
            BrokerEnvelopeParser(objectMapper).parse("ignored".toByteArray())
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
