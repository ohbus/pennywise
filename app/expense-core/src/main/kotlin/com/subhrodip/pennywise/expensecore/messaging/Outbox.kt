package com.subhrodip.pennywise.expensecore.messaging

import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class OutboxStatus { PENDING, CLAIMED, PUBLISHED, PARKED }

data class OutboxMessage(
    val eventId: UUID,
    val eventType: String,
    val aggregateId: UUID,
    val groupId: UUID,
    val groupRevision: Long,
    val occurredAt: Instant,
    val payload: Map<String, Any?>,
    var status: OutboxStatus = OutboxStatus.PENDING,
    var attempts: Int = 0,
    var leaseUntil: Instant? = null,
    var availableAt: Instant = occurredAt
)

/** Writer-side transactional outbox command port. */
interface OutboxCommandStore {
    fun append(message: OutboxMessage)

    fun claim(limit: Int, lease: Duration): List<OutboxMessage>

    fun acknowledge(eventId: UUID)

    fun reject(eventId: UUID, maxAttempts: Int, retryAfter: Duration)
}

/** Read-only outbox inspection port. */
interface OutboxQueryStore {
    fun snapshot(): List<OutboxMessage>
}

/** Compatibility facade combining outbox command and query operations. */
interface OutboxStore : OutboxCommandStore, OutboxQueryStore

/** Deterministic in-memory adapter used by domain tests. */
class OutboxRelay(private val clock: () -> Instant = Instant::now) : OutboxStore {
    private val messages = ConcurrentHashMap<UUID, OutboxMessage>()

    override fun append(message: OutboxMessage) {
        require(messages.putIfAbsent(message.eventId, message) == null) { "eventId already exists" }
    }

    @Synchronized
    override fun claim(limit: Int, lease: Duration): List<OutboxMessage> {
        require(limit > 0)
        val now = clock()
        return messages.values.asSequence()
            .filter { (it.status == OutboxStatus.PENDING && !it.availableAt.isAfter(now)) || (it.status == OutboxStatus.CLAIMED && it.leaseUntil?.isBefore(now) == true) }
            .sortedBy { it.occurredAt }.take(limit).onEach {
                it.status = OutboxStatus.CLAIMED; it.leaseUntil = now.plus(lease); it.attempts++
            }.toList()
    }

    @Synchronized
    override fun acknowledge(eventId: UUID) { messages[eventId]?.apply { status = OutboxStatus.PUBLISHED; leaseUntil = null } }

    /** Marks a failed publish for retry, or parks it after the configured attempt limit. */
    @Synchronized
    override fun reject(eventId: UUID, maxAttempts: Int, retryAfter: Duration) {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(!retryAfter.isNegative) { "retryAfter must not be negative" }
        messages[eventId]?.apply {
            leaseUntil = null
            if (attempts >= maxAttempts) {
                status = OutboxStatus.PARKED
            } else {
                status = OutboxStatus.PENDING
                availableAt = clock().plus(retryAfter)
            }
        }
    }

    override fun snapshot(): List<OutboxMessage> = messages.values.sortedBy { it.occurredAt }
}
