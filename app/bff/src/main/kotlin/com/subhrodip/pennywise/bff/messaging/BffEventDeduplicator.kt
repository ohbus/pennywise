package com.subhrodip.pennywise.bff.messaging

import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

/**
 * Thread-safe bounded LRU deduplicator for broker events received by a BFF replica.
 *
 * Prevents redundant live-update fanout and duplicate invalidation triggers
 * when duplicate broker deliveries or replayed events occur.
 */
class BffEventDeduplicator(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val seenEvents: MutableMap<UUID, Boolean> = Collections.synchronizedMap(
        object : LinkedHashMap<UUID, Boolean>(capacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Boolean>?): Boolean {
                return size > capacity
            }
        }
    )

    /**
     * Checks if the event has been seen before, and marks it as seen if not.
     * Accesses the entry to update its LRU position if already present.
     *
     * @param eventId unique event identifier
     * @return true if the event is a duplicate (already seen), false if it is newly seen
     */
    fun isDuplicateAndMark(eventId: UUID): Boolean {
        synchronized(seenEvents) {
            return if (seenEvents[eventId] != null) {
                true
            } else {
                seenEvents[eventId] = true
                false
            }
        }
    }

    /**
     * Clears all recorded event IDs from the cache.
     */
    fun clear() {
        seenEvents.clear()
    }

    /**
     * Current count of tracked event IDs.
     */
    fun size(): Int = seenEvents.size

    companion object {
        const val DEFAULT_CAPACITY = 10_000
    }
}
