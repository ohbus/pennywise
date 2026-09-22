package com.subhrodip.pennywise.notifications.delivery.persistence

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Deterministic adapter for unit tests and explicitly in-memory use. */
class InboxDeduplicator : EventDeduplicator {
    private val processed = ConcurrentHashMap.newKeySet<UUID>()

    override fun firstDelivery(eventId: UUID): Boolean = processed.add(eventId)

    fun size(): Int = processed.size
}
