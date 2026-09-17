package com.subhrodip.pennywise.notifications

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface EventDeduplicator {
    fun firstDelivery(eventId: UUID): Boolean
}

/** Deterministic adapter for unit tests and explicitly in-memory use. */
class InboxDeduplicator : EventDeduplicator {
    private val processed = ConcurrentHashMap.newKeySet<UUID>()

    override fun firstDelivery(eventId: UUID): Boolean = processed.add(eventId)

    fun size(): Int = processed.size
}
