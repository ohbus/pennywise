package com.subhrodip.pennywise.expensecore.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest
class JpaSynchronizationStoreTest @Autowired constructor(
    private val store: JpaSynchronizationStore
) {
    @Test
    fun `persists revisions sequentially and preserves deletion tombstones`() {
        val groupId = UUID.randomUUID().toString()

        val rev1 = store.append(groupId, "expense-1", """{"amount": 100}""")
        val rev2 = store.append(groupId, "expense-2", """{"amount": 200}""")
        val rev3 = store.delete(groupId, "expense-1")

        assertEquals(1L, rev1)
        assertEquals(2L, rev2)
        assertEquals(3L, rev3)

        val snapshot = store.snapshot(groupId, null, 10)
        assertEquals(3, snapshot.changes.size)
        assertFalse(snapshot.hasMore)

        val first = snapshot.changes[0]
        assertEquals(1L, first.revision)
        assertEquals("expense-1", first.entityId)
        assertFalse(first.deleted)
        assertEquals("""{"amount": 100}""", first.payload)

        val second = snapshot.changes[1]
        assertEquals(2L, second.revision)
        assertEquals("expense-2", second.entityId)
        assertFalse(second.deleted)

        val third = snapshot.changes[2]
        assertEquals(3L, third.revision)
        assertEquals("expense-1", third.entityId)
        assertTrue(third.deleted)
        assertNull(third.payload)
    }

    @Test
    fun `pages changes with opaque cursors across multiple pages`() {
        val groupId = UUID.randomUUID().toString()

        for (i in 1..5) {
            store.append(groupId, "item-$i", """{"index": $i}""")
        }

        val page1 = store.snapshot(groupId, null, 2)
        assertEquals(2, page1.changes.size)
        assertEquals(listOf(1L, 2L), page1.changes.map { it.revision })
        assertTrue(page1.hasMore)
        assertNotNull(page1.nextCursor)

        val page2 = store.snapshot(groupId, page1.nextCursor, 2)
        assertEquals(2, page2.changes.size)
        assertEquals(listOf(3L, 4L), page2.changes.map { it.revision })
        assertTrue(page2.hasMore)
        assertNotNull(page2.nextCursor)

        val page3 = store.snapshot(groupId, page2.nextCursor, 2)
        assertEquals(1, page3.changes.size)
        assertEquals(listOf(5L), page3.changes.map { it.revision })
        assertFalse(page3.hasMore)
    }

    @Test
    fun `cursor cannot be replayed for another group`() {
        val groupA = UUID.randomUUID().toString()
        val groupB = UUID.randomUUID().toString()

        store.append(groupA, "item-1", "{}")
        val cursorA = store.snapshot(groupA, null, 1).nextCursor

        assertThrows(InvalidSyncCursorException::class.java) {
            store.snapshot(groupB, cursorA, 1)
        }
    }

    @Test
    fun `groups have independent revision sequences`() {
        val group1 = UUID.randomUUID().toString()
        val group2 = UUID.randomUUID().toString()

        val g1r1 = store.append(group1, "e-1", "{}")
        val g2r1 = store.append(group2, "e-2", "{}")
        val g1r2 = store.append(group1, "e-3", "{}")

        assertEquals(1L, g1r1)
        assertEquals(1L, g2r1)
        assertEquals(2L, g1r2)
    }
}
