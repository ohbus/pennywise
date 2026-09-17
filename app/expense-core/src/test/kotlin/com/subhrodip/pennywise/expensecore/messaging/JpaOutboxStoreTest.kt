package com.subhrodip.pennywise.expensecore.messaging

import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration tests for [JpaOutboxStore] verifying transactional claim, retry,
 * acknowledgement, parking, and concurrency semantics against the persistent outbox table.
 *
 * Each test purges the outbox table via [setUp] to ensure isolation from other
 * integration tests executing in the shared Spring Boot context.
 */
@SpringBootTest
class JpaOutboxStoreTest @Autowired constructor(
    private val store: JpaOutboxStore,
    private val repository: OutboxRepository
) {
    /**
     * Purges all existing outbox messages prior to each test invocation, guaranteeing
     * an isolated baseline independent of test execution order.
     */
    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    /**
     * Verifies that outbox events transition cleanly through claim, retry backoff,
     * final parking on exceeding maximum attempts, and successful acknowledgement.
     */
    @Test
    fun `persists claim retry acknowledgement and parking state`() {
        val retryId = UUID.randomUUID()
        val publishedId = UUID.randomUUID()
        store.append(message(retryId, Instant.now().minusSeconds(2), mapOf("amount" to 1250, "currency" to "EUR")))
        store.append(message(publishedId, Instant.now().minusSeconds(1)))

        val firstClaim = store.claim(1, Duration.ofMinutes(1)).single()
        assertEquals(retryId, firstClaim.eventId)
        assertEquals(1, firstClaim.attempts)
        store.reject(retryId, maxAttempts = 2, retryAfter = Duration.ZERO)

        val retried = store.claim(1, Duration.ofMinutes(1)).single()
        assertEquals(retryId, retried.eventId)
        assertEquals(2, retried.attempts)
        store.reject(retryId, maxAttempts = 2, retryAfter = Duration.ZERO)

        val nextClaim = store.claim(1, Duration.ofMinutes(1)).single()
        assertEquals(publishedId, nextClaim.eventId)
        store.acknowledge(publishedId)

        val snapshot = store.snapshot().associateBy { it.eventId }
        assertEquals(OutboxStatus.PARKED, snapshot.getValue(retryId).status)
        assertEquals(OutboxStatus.PUBLISHED, snapshot.getValue(publishedId).status)
        assertEquals(mapOf("amount" to 1250, "currency" to "EUR"), snapshot.getValue(retryId).payload)
        assertTrue(store.claim(10, Duration.ofMinutes(1)).isEmpty())
    }

    /**
     * Verifies that concurrent threads executing batch claims against the persistent
     * outbox using SELECT FOR UPDATE SKIP LOCKED claim disjoint event sets without duplicates.
     */
    @Test
    fun `competing workers claim each event once`() {
        val ids = (1..12).map { index ->
            UUID.randomUUID().also { store.append(message(it, Instant.now().minusSeconds(20L - index))) }
        }.toSet()
        val executor = Executors.newFixedThreadPool(3)
        val start = CountDownLatch(1)
        val claims = (1..3).map {
            executor.submit(Callable {
                start.await()
                store.claim(12, Duration.ofMinutes(1)).map(OutboxMessage::eventId)
            })
        }
        start.countDown()
        val claimedIds = claims.flatMap { it.get() }
        executor.shutdown()

        assertEquals(ids, claimedIds.toSet())
        assertEquals(ids.size, claimedIds.size)
        assertTrue(store.claim(12, Duration.ofMinutes(1)).isEmpty())
    }

    /**
     * Verifies that when an outbox message lease expires, it becomes eligible for
     * reclaiming by subsequent worker claim operations with an incremented attempt counter.
     */
    @Test
    fun `expired persisted lease is reclaimable`() {
        val eventId = UUID.randomUUID()
        store.append(message(eventId, Instant.now().minusSeconds(10)).copy(
            status = OutboxStatus.CLAIMED,
            attempts = 1,
            leaseUntil = Instant.now().minusSeconds(1)
        ))

        val reclaimed = store.claim(1, Duration.ofMinutes(1)).single()

        assertEquals(eventId, reclaimed.eventId)
        assertEquals(2, reclaimed.attempts)
        assertEquals(OutboxStatus.CLAIMED, reclaimed.status)
    }

    /**
     * Creates a test [OutboxMessage] fixture with the specified parameters.
     */
    private fun message(
        eventId: UUID,
        occurredAt: Instant,
        payload: Map<String, Any?> = emptyMap()
    ) = OutboxMessage(
        eventId = eventId,
        eventType = "expense.created",
        aggregateId = UUID.randomUUID(),
        groupId = UUID.randomUUID(),
        groupRevision = 1,
        occurredAt = occurredAt,
        payload = payload
    )
}
