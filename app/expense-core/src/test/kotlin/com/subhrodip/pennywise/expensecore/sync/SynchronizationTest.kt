package com.subhrodip.pennywise.expensecore.sync

import com.subhrodip.pennywise.expensecore.sync.domain.InvalidSyncCursorException
import com.subhrodip.pennywise.expensecore.sync.domain.SyncCursor
import com.subhrodip.pennywise.expensecore.sync.persistence.InMemorySynchronizationStore

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SynchronizationTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `pages changes in revision order and propagates deletion`() {
        val store = InMemorySynchronizationStore(clock)
        store.append("a", "one")
        store.append("b", "two")
        store.delete("a")
        val first = store.snapshot(null, 2)
        assertEquals(listOf(1L, 2L), first.changes.map { it.revision })
        assertTrue(first.hasMore)
        val second = store.snapshot(first.nextCursor, 2)
        assertEquals(3L, second.changes.single().revision)
        assertTrue(second.changes.single().deleted)
    }

    @Test
    fun `cursor round trips and expires`() {
        val store = InMemorySynchronizationStore(clock, Duration.ofMinutes(5))
        store.append("a", "one")
        val cursor = store.snapshot(null, 1).nextCursor!!
        assertEquals(1L, SyncCursor.decode(cursor).revision)
        assertEquals("default", SyncCursor.decode(cursor).groupId)
        val expiredClock = Clock.fixed(now.plusSeconds(301), ZoneOffset.UTC)
        assertThrows(InvalidSyncCursorException::class.java) { InMemorySynchronizationStore(expiredClock).snapshot(cursor, 1) }
    }

    @Test
    fun `invalid cursor and invalid limits are rejected`() {
        val store = InMemorySynchronizationStore(clock)
        assertThrows(InvalidSyncCursorException::class.java) { store.snapshot("bad", 1) }
        assertThrows(IllegalArgumentException::class.java) { store.snapshot(null, 0) }
        assertThrows(IllegalArgumentException::class.java) { store.snapshot(null, 101) }
    }

    @Test
    fun `retrying after a lost response returns changes after the cursor`() {
        val store = InMemorySynchronizationStore(clock)
        store.append("group", "a", "one")
        val first = store.snapshot("group", null, 1)
        store.append("group", "b", "two")
        assertEquals(listOf("b"), store.snapshot("group", first.nextCursor, 10).changes.map { it.entityId })
    }

    @Test
    fun `cursor cannot be replayed for another group`() {
        val store = InMemorySynchronizationStore(clock)
        store.append("one", "a", "one")
        val cursor = store.snapshot("one", null, 1).nextCursor
        assertThrows(InvalidSyncCursorException::class.java) { store.snapshot("two", cursor, 1) }
    }
}
