package com.subhrodip.pennywise.expensecore.messaging.broker

/** Deterministic broker adapter used only by isolated tests. */
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
