package com.subhrodip.pennywise.notifications.delivery

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class DeliveryRateLimiter(private val limit: Int = 10, private val window: Duration = Duration.ofMinutes(1), private val clock: () -> Instant = Instant::now) {
    private val counters = ConcurrentHashMap<String, Window>()
    fun allow(subject: String): Boolean {
        val now = clock()
        val current = counters.compute(subject) { _, old ->
            if (old == null || Duration.between(old.startedAt, now) >= window) Window(now, 1)
            else old.copy(count = old.count + 1)
        }!!
        return current.count <= limit
    }
    private data class Window(val startedAt: Instant, val count: Int)
}
