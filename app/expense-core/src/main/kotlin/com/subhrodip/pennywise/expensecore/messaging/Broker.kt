package com.subhrodip.pennywise.expensecore.messaging

import java.time.Instant
import java.util.UUID

data class BrokerMessage(val eventId: UUID, val eventType: String, val payload: ByteArray, val occurredAt: Instant, val headers: Map<String, String> = emptyMap())

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
