package com.subhrodip.pennywise.bff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class LiveUpdateFanoutTest {
    @Test
    fun `bounds subscriptions per user`() {
        val fanout = LiveUpdateFanout(maxSubscriptionsPerUser = 1)
        fanout.subscribe("user-1", "group-1")

        assertThrows<IllegalArgumentException> { fanout.subscribe("user-1", "group-2") }
    }

    @Test
    fun `concurrent subscription admission cannot exceed user limit`() {
        val fanout = LiveUpdateFanout(maxSubscriptionsPerUser = 1)
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val attempts = (1..8).map {
                pool.submit<Boolean> {
                    start.await()
                    runCatching { fanout.subscribe("user-1", "group-$it") }.isSuccess
                }
            }
            start.countDown()

            assertThat(attempts.count { it.get() }).isEqualTo(1)
        } finally {
            pool.shutdownNow()
        }
    }
    private val update = LiveUpdate("group-1", 7)

    @Test
    fun `delivers update only to subscriptions for the group`() {
        val fanout = LiveUpdateFanout()
        val matching = fanout.subscribe("user-1", "group-1")
        val other = fanout.subscribe("user-2", "group-2")

        assertThat(fanout.publish(update)).isEqualTo(1)
        assertThat(fanout.poll(matching.id)).isEqualTo(update)
        assertThat(fanout.poll(other.id)).isNull()
    }

    @Test
    fun `bounds slow subscriber queue and preserves queued order`() {
        val fanout = LiveUpdateFanout(queueCapacity = 2)
        val subscription = fanout.subscribe("user-1", "group-1")

        assertThat(fanout.publish(update)).isEqualTo(1)
        assertThat(fanout.publish(update.copy(revision = 8))).isEqualTo(1)
        assertThat(fanout.publish(update.copy(revision = 9))).isZero()
        assertThat(fanout.pendingCount(subscription.id)).isEqualTo(2)
        assertThat(fanout.poll(subscription.id)?.revision).isEqualTo(7)
        assertThat(fanout.poll(subscription.id)?.revision).isEqualTo(8)
    }

    @Test
    fun `unsubscribe drops future delivery`() {
        val fanout = LiveUpdateFanout()
        val subscription = fanout.subscribe("user-1", "group-1")
        fanout.unsubscribe(subscription.id)

        assertThat(fanout.publish(update)).isZero()
        assertThat(fanout.poll(subscription.id)).isNull()
    }

    @Test
    fun `rejects invalid inputs`() {
        assertThrows<IllegalArgumentException> { LiveUpdateFanout(0) }
        val fanout = LiveUpdateFanout()
        assertThrows<IllegalArgumentException> { fanout.subscribe("", "group-1") }
        assertThrows<IllegalArgumentException> { fanout.publish(LiveUpdate("group-1", -1)) }
    }

    @Test
    fun `expires subscriptions before delivery`() {
        val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
        val fanout = LiveUpdateFanout(clock = clock, subscriptionTtl = Duration.ofSeconds(1))
        val subscription = fanout.subscribe("user-1", "group-1")

        assertThat(subscription.expiresAt).isEqualTo(Instant.parse("2026-01-01T00:00:01Z"))
        assertThat(fanout.publish(update)).isEqualTo(1)
    }

    @Test
    fun `revokes every subscription for a user`() {
        val fanout = LiveUpdateFanout()
        fanout.subscribe("user-1", "group-1")
        fanout.subscribe("user-1", "group-2")
        fanout.subscribe("user-2", "group-1")

        assertThat(fanout.revokeUser("user-1")).isEqualTo(2)
        assertThat(fanout.publish(update)).isEqualTo(1)
        assertThrows<IllegalArgumentException> { fanout.revokeUser(" ") }
    }

    @Test
    fun `emitInvalidation publishes to reactive invalidations flux`() {
        val fanout = LiveUpdateFanout()
        val events = mutableListOf<GroupInvalidation>()
        val disposable = fanout.invalidations().subscribe { events.add(it) }
        try {
            val invalidation = fanout.emitInvalidation("group-1", 10L, "change-abc")
            assertThat(invalidation.groupId).isEqualTo("group-1")
            assertThat(invalidation.revision).isEqualTo(10L)
            assertThat(invalidation.changeId).isEqualTo("change-abc")
            assertThat(events).hasSize(1)
            assertThat(events[0]).isEqualTo(invalidation)
        } finally {
            disposable.dispose()
        }
    }
}
