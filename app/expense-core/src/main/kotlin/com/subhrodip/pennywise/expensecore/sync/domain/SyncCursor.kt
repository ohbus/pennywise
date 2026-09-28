package com.subhrodip.pennywise.expensecore.sync.domain

import java.time.Instant
import java.util.Base64

/** Opaque, expiring cursor for a group synchronization stream. */
data class SyncCursor(val groupId: String, val revision: Long, val expiresAt: Instant) {
    /** Encodes this cursor for transport. */
    fun encode(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("$groupId|$revision|${expiresAt.toEpochMilli()}".toByteArray())

    companion object {
        /** Decodes a cursor and normalizes malformed values to the domain exception. */
        fun decode(value: String): SyncCursor = try {
            val decoded = String(Base64.getUrlDecoder().decode(value)).split('|')
            require(decoded.size == 3 && decoded[0].isNotBlank())
            SyncCursor(decoded[0], decoded[1].toLong(), Instant.ofEpochMilli(decoded[2].toLong()))
        } catch (exception: RuntimeException) {
            throw InvalidSyncCursorException()
        }
    }
}
