package com.subhrodip.pennywise.expensecore.sync

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import org.springframework.stereotype.Service

data class SyncCursor(val groupId: String, val revision: Long, val expiresAt: Instant) {
    fun encode(): String {
        val value = "$groupId|$revision|${expiresAt.toEpochMilli()}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    }

    companion object {
        fun decode(value: String): SyncCursor {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(value)).split('|')
                require(decoded.size == 3 && decoded[0].isNotBlank())
                SyncCursor(decoded[0], decoded[1].toLong(), Instant.ofEpochMilli(decoded[2].toLong()))
            } catch (exception: RuntimeException) {
                throw InvalidSyncCursorException()
            }
        }
    }
}

class InvalidSyncCursorException : RuntimeException("The synchronization cursor is invalid or expired")

data class SyncChange(val revision: Long, val entityId: String, val deleted: Boolean, val payload: String?)

data class SyncPage(val changes: List<SyncChange>, val nextCursor: String?, val hasMore: Boolean)

interface SynchronizationStore {
    fun append(entityId: String, payload: String?): Long = append("default", entityId, payload)
    fun append(groupId: String, entityId: String, payload: String?): Long
    fun delete(entityId: String): Long = delete("default", entityId)
    fun delete(groupId: String, entityId: String): Long
    fun snapshot(after: String?, limit: Int): SyncPage = snapshot("default", after, limit)
    fun snapshot(groupId: String, after: String?, limit: Int): SyncPage

    companion object {
        operator fun invoke(
            clock: Clock = Clock.systemUTC(),
            cursorLifetime: Duration = Duration.ofHours(24)
        ): SynchronizationStore = InMemorySynchronizationStore(clock, cursorLifetime)
    }
}

@Service
class InMemorySynchronizationStore(
    private val clock: Clock = Clock.systemUTC(),
    private val cursorLifetime: Duration = Duration.ofHours(24)
) : SynchronizationStore {
    private val changes = mutableMapOf<String, MutableList<SyncChange>>()
    private var revision = 0L

    @Synchronized
    override fun append(entityId: String, payload: String?): Long = append("default", entityId, payload)

    @Synchronized
    override fun append(groupId: String, entityId: String, payload: String?): Long {
        require(groupId.isNotBlank())
        require(entityId.isNotBlank())
        return record(groupId, SyncChange(nextRevision(groupId), entityId, false, payload))
    }

    @Synchronized
    override fun delete(entityId: String): Long = delete("default", entityId)

    @Synchronized
    override fun delete(groupId: String, entityId: String): Long {
        require(groupId.isNotBlank())
        require(entityId.isNotBlank())
        return record(groupId, SyncChange(nextRevision(groupId), entityId, true, null))
    }

    @Synchronized
    override fun snapshot(after: String?, limit: Int): SyncPage = snapshot("default", after, limit)

    @Synchronized
    override fun snapshot(groupId: String, after: String?, limit: Int): SyncPage {
        require(groupId.isNotBlank())
        require(limit in 1..100)
        val start = validate(groupId, after)
        val groupChanges = changes[groupId].orEmpty()
        val page = groupChanges.filter { it.revision > start }.take(limit)
        return page(groupId, start, groupChanges, page)
    }

    private fun validate(groupId: String, cursor: String?): Long {
        if (cursor == null) return 0
        val decoded = SyncCursor.decode(cursor)
        if (decoded.groupId != groupId) throw InvalidSyncCursorException()
        if (!decoded.expiresAt.isAfter(clock.instant())) throw InvalidSyncCursorException()
        return decoded.revision
    }

    private fun page(groupId: String, start: Long, all: List<SyncChange>, page: List<SyncChange>): SyncPage {
        val hasMore = all.any { it.revision > start + page.size }
        val next = page.lastOrNull()?.let { SyncCursor(groupId, it.revision, clock.instant().plus(cursorLifetime)).encode() }
        return SyncPage(page, next, hasMore)
    }

    private fun nextRevision(groupId: String): Long = (changes[groupId]?.lastOrNull()?.revision ?: 0) + 1

    private fun record(groupId: String, change: SyncChange): Long {
        changes.getOrPut(groupId) { mutableListOf() }.add(change)
        return change.revision
    }
}
