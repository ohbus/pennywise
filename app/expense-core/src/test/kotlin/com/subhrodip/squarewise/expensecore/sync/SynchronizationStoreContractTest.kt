package com.subhrodip.squarewise.expensecore.sync

import com.subhrodip.squarewise.expensecore.sync.domain.SyncPage
import com.subhrodip.squarewise.expensecore.sync.persistence.SynchronizationCommandStore
import com.subhrodip.squarewise.expensecore.sync.persistence.SynchronizationQueryStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies synchronization persistence-port convenience overloads preserve their defaults. */
class SynchronizationStoreContractTest {
    @Test
    fun `command convenience overloads use the default group`() {
        val store = RecordingCommandStore()

        assertEquals(4L, store.append("expense-1", "payload"))
        assertEquals(5L, store.delete("expense-1"))
        assertEquals(listOf("default" to "expense-1"), store.appendCalls)
        assertEquals(listOf("default" to "expense-1"), store.deleteCalls)
    }

    @Test
    fun `query convenience overload uses the default group`() {
        val store = RecordingQueryStore()

        val page = store.snapshot(after = "cursor-1", limit = 25)

        assertEquals(SyncPage(emptyList(), "cursor-2", hasMore = true), page)
        assertEquals("default", store.groupId)
        assertEquals("cursor-1", store.after)
        assertEquals(25, store.limit)
    }

    private class RecordingCommandStore : SynchronizationCommandStore {
        val appendCalls = mutableListOf<Pair<String, String>>()
        val deleteCalls = mutableListOf<Pair<String, String>>()

        override fun append(groupId: String, entityId: String, payload: String?): Long {
            appendCalls += groupId to entityId
            return 4L
        }

        override fun delete(groupId: String, entityId: String): Long {
            deleteCalls += groupId to entityId
            return 5L
        }
    }

    private class RecordingQueryStore : SynchronizationQueryStore {
        var groupId: String? = null
        var after: String? = null
        var limit: Int? = null

        override fun snapshot(groupId: String, after: String?, limit: Int): SyncPage {
            this.groupId = groupId
            this.after = after
            this.limit = limit
            return SyncPage(emptyList(), "cursor-2", hasMore = true)
        }
    }
}
