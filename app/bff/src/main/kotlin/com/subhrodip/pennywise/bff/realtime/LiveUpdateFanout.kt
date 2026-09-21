package com.subhrodip.pennywise.bff

import com.subhrodip.pennywise.ids.UuidGenerator
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class GroupInvalidation(
    val groupId: String,
    val revision: Long,
    val changeId: String = UuidGenerator.next().toString()
)

data class LiveUpdate(val groupId: String, val revision: Long)

data class LiveSubscription(
    val id: String,
    val userId: String,
    val groupId: String,
    val expiresAt: Instant
)

class LiveUpdateFanout(
    private val queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    private val clock: Clock = Clock.systemUTC(),
    private val subscriptionTtl: Duration = DEFAULT_SUBSCRIPTION_TTL,
    private val maxSubscriptionsPerUser: Int = DEFAULT_MAX_SUBSCRIPTIONS_PER_USER
) {
    private val subscriptions = ConcurrentHashMap<String, Subscriber>()
    private val admissionLock = Any()
    val invalidationSink: Sinks.Many<GroupInvalidation> =
        Sinks.many().multicast().directBestEffort()

    fun emitInvalidation(groupId: String, revision: Long): GroupInvalidation =
        emitInvalidation(groupId, revision, UuidGenerator.next().toString())

    fun emitInvalidation(groupId: String, revision: Long, changeId: String): GroupInvalidation {
        val invalidation = GroupInvalidation(groupId, revision, changeId)
        invalidationSink.tryEmitNext(invalidation)
        return invalidation
    }

    fun invalidations(): Flux<GroupInvalidation> = invalidationSink.asFlux()

    init {
        require(queueCapacity > 0) { "queueCapacity must be positive" }
        require(!subscriptionTtl.isZero && !subscriptionTtl.isNegative) { "subscriptionTtl must be positive" }
        require(maxSubscriptionsPerUser > 0) { "maxSubscriptionsPerUser must be positive" }
    }

    fun subscribe(userId: String, groupId: String): LiveSubscription {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(groupId.isNotBlank()) { "groupId must not be blank" }
        return synchronized(admissionLock) {
            removeExpired()
            require(subscriptions.values.count { it.subscription.userId == userId } < maxSubscriptionsPerUser) {
                "subscription limit exceeded"
            }
            val subscription = LiveSubscription(
                UuidGenerator.next().toString(), userId, groupId, Instant.now(clock).plus(subscriptionTtl)
            )
            subscriptions[subscription.id] = Subscriber(subscription, ArrayDeque())
            subscription
        }
    }

    fun unsubscribe(subscriptionId: String) {
        subscriptions.remove(subscriptionId)
    }

    fun revokeUser(userId: String): Int {
        require(userId.isNotBlank()) { "userId must not be blank" }
        var revoked = 0
        subscriptions.entries.removeIf {
            val matches = it.value.subscription.userId == userId
            if (matches) revoked++
            matches
        }
        return revoked
    }

    /** Revokes only the removed subject's subscriptions for the affected group. */
    fun revokeUserFromGroup(userId: String, groupId: String): Int {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(groupId.isNotBlank()) { "groupId must not be blank" }
        var revoked = 0
        subscriptions.entries.removeIf {
            val subscription = it.value.subscription
            val matches = subscription.userId == userId && subscription.groupId == groupId
            if (matches) revoked++
            matches
        }
        return revoked
    }

    fun publish(update: LiveUpdate): Int {
        require(update.groupId.isNotBlank()) { "groupId must not be blank" }
        require(update.revision >= 0) { "revision must not be negative" }
        removeExpired()
        var delivered = 0
        subscriptions.values.forEach { subscriber ->
            if (subscriber.subscription.groupId == update.groupId && subscriber.offer(update, queueCapacity)) {
                delivered++
            }
        }
        return delivered
    }

    fun poll(subscriptionId: String): LiveUpdate? {
        removeExpired()
        return subscriptions[subscriptionId]?.poll()
    }

    fun pendingCount(subscriptionId: String): Int {
        removeExpired()
        return subscriptions[subscriptionId]?.size() ?: 0
    }

    private fun removeExpired() {
        val now = Instant.now(clock)
        subscriptions.entries.removeIf { !now.isBefore(it.value.subscription.expiresAt) }
    }

    private class Subscriber(val subscription: LiveSubscription, private val queue: ArrayDeque<LiveUpdate>) {
        @Synchronized
        fun offer(update: LiveUpdate, capacity: Int): Boolean {
            if (queue.size >= capacity) return false
            queue.addLast(update)
            return true
        }

        @Synchronized
        fun poll(): LiveUpdate? = if (queue.isEmpty()) null else queue.removeFirst()

        @Synchronized
        fun size(): Int = queue.size
    }

    companion object {
        const val DEFAULT_QUEUE_CAPACITY = 64
        const val DEFAULT_MAX_SUBSCRIPTIONS_PER_USER = 20
        val DEFAULT_SUBSCRIPTION_TTL: Duration = Duration.ofMinutes(30)
    }
}
