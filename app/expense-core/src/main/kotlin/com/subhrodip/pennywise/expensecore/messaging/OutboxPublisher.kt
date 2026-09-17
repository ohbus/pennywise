package com.subhrodip.pennywise.expensecore.messaging

import java.nio.charset.StandardCharsets
import java.time.Duration

/** Publishes leased outbox records and changes state only after a broker confirm. */
class OutboxPublisher(
    private val relay: OutboxStore,
    private val publisher: BrokerPublisher,
    private val batchSize: Int = 100,
    private val lease: Duration = Duration.ofSeconds(30),
    private val maxAttempts: Int = 5,
    private val retryAfter: Duration = Duration.ofSeconds(5)
) {
    init {
        require(batchSize > 0) { "batchSize must be positive" }
        require(!lease.isNegative && !lease.isZero) { "lease must be positive" }
        require(maxAttempts > 0) { "maxAttempts must be positive" }
    }

    fun publishAvailable(): PublishBatchResult {
        val claimed = relay.claim(batchSize, lease)
        var confirmed = 0
        var rejected = 0
        claimed.forEach { message ->
            val result = publisher.publish(message.toBrokerMessage())
            when (result) {
                PublishResult.Confirmed -> {
                    relay.acknowledge(message.eventId)
                    confirmed++
                }
                is PublishResult.Rejected -> {
                    relay.reject(message.eventId, maxAttempts, retryAfter)
                    rejected++
                }
            }
        }
        return PublishBatchResult(claimed.size, confirmed, rejected)
    }
}

data class PublishBatchResult(val claimed: Int, val confirmed: Int, val rejected: Int)

private fun OutboxMessage.toBrokerMessage(): BrokerMessage = BrokerMessage(
    eventId = eventId,
    eventType = eventType,
    payload = payload.entries
        .sortedBy { it.key }
        .joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":${it.value}" }
        .toByteArray(StandardCharsets.UTF_8),
    occurredAt = occurredAt,
    headers = mapOf(
        "aggregate-id" to aggregateId.toString(),
        "group-id" to groupId.toString(),
        "group-revision" to groupRevision.toString()
    )
)
