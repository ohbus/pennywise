package com.subhrodip.pennywise.expensecore.expenses

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class ExpenseIdempotencyCleanupTest {
    @Test
    fun `deletes one bounded expired claim batch`() {
        val repository = mock(ExpenseIdempotencyRepository::class.java)
        val now = Instant.parse("2026-09-21T00:00:00Z")
        val claim = ExpenseIdempotencyEntity(UUID.randomUUID(), UUID.randomUUID(), "alice", "CREATE", "key", "hash", UUID.randomUUID(), now.minusSeconds(90_000))
        `when`(repository.findByCreatedAtBefore(now.minus(Duration.ofHours(24)), PageRequest.of(0, 1)))
            .thenReturn(listOf(claim))
        val cleanup = ExpenseIdempotencyCleanup(repository, Clock.fixed(now, ZoneOffset.UTC), Duration.ofHours(24), 1)

        assertEquals(1, cleanup.cleanupExpired())
        verify(repository).deleteAllById(listOf(claim.idempotencyId))
    }
}
