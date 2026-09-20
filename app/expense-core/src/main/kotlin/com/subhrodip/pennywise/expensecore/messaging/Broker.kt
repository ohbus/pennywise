package com.subhrodip.pennywise.expensecore.messaging

import java.time.Instant
import java.util.UUID

data class BrokerMessage(
    val eventId: UUID,
    val eventType: String,
    val payload: ByteArray,
    val occurredAt: Instant,
    val headers: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BrokerMessage

        if (eventId != other.eventId) return false
        if (eventType != other.eventType) return false
        if (!payload.contentEquals(other.payload)) return false
        if (occurredAt != other.occurredAt) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + eventType.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + occurredAt.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }

    override fun toString(): String =
        "BrokerMessage(eventId=$eventId, eventType='$eventType', payload=${payload.contentToString()}, occurredAt=$occurredAt, headers=$headers)"
}

sealed interface PublishResult {
    data object Confirmed : PublishResult
    data class Rejected(val reason: String) : PublishResult
}

fun interface BrokerPublisher { fun publish(message: BrokerMessage): PublishResult }

interface BrokerConsumer { fun receive(handler: (BrokerMessage) -> Boolean) }

class InMemoryBroker : BrokerPublisher, BrokerConsumer {
    private val messages = mutableListOf<BrokerMessage>()

    @Synchronized override fun publish(message: BrokerMessage): PublishResult {
        if (messages.none { it.eventId == message.eventId }) messages += message
        return PublishResult.Confirmed
    }

    @Synchronized override fun receive(handler: (BrokerMessage) -> Boolean) {
        messages.toList().forEach { message -> if (handler(message)) messages.removeIf { it.eventId == message.eventId } }
    }

    @Synchronized fun size(): Int = messages.size
}
