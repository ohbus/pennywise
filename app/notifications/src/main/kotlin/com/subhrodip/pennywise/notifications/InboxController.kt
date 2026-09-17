package com.subhrodip.pennywise.notifications

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class InboxItem(val notificationId: UUID, val eventType: String, val message: String, val occurredAt: Instant, val read: Boolean = false)
data class InboxPage(val items: List<InboxItem>, val nextCursor: String? = null)

@RestController
@RequestMapping("/notifications/v1/inbox")
class InboxController(private val inbox: NotificationInbox) {
    @GetMapping
    fun list(
        principal: Principal,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "50") limit: Int
    ): InboxPage {
        val subject = principal.name.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("authenticated subject is required")
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return inbox.page(subject, cursor, limit)
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAsRead(@PathVariable notificationId: UUID, principal: Principal) {
        val subject = principal.name.trim().takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated subject is required")
        if (!inbox.markAsRead(subject, notificationId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")
        }
    }
}

@Service
class NotificationInbox(
    private val store: NotificationInboxStore = InMemoryNotificationInboxStore()
) {
    fun append(subject: String, item: InboxItem) = store.append(subject, item)

    fun list(subject: String): List<InboxItem> = store.list(subject)

    fun markAsRead(subject: String, notificationId: UUID): Boolean = store.markAsRead(subject, notificationId)

    fun page(subject: String, cursor: String?, limit: Int): InboxPage {
        val sorted = list(subject).sortedWith(compareByDescending<InboxItem> { it.occurredAt }.thenByDescending { it.notificationId })
        val start = cursor?.let { decodeCursor(it) }?.let { key ->
            sorted.indexOfFirst { it.occurredAt < key.first || (it.occurredAt == key.first && it.notificationId < key.second) }
                .takeIf { it >= 0 } ?: sorted.size
        } ?: 0
        val page = sorted.drop(start).take(limit)
        val next = if (start + page.size < sorted.size) encodeCursor(page.last()) else null
        return InboxPage(page, next)
    }

    private fun encodeCursor(item: InboxItem): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("${item.occurredAt}|${item.notificationId}".toByteArray())

    private fun decodeCursor(cursor: String): Pair<Instant, UUID> = runCatching {
        val value = String(Base64.getUrlDecoder().decode(cursor)).split('|')
        require(value.size == 2)
        Instant.parse(value[0]) to UUID.fromString(value[1])
    }.getOrElse { throw IllegalArgumentException("invalid inbox cursor") }
}

interface NotificationInboxStore {
    fun append(subject: String, item: InboxItem)
    fun list(subject: String): List<InboxItem>
    fun markAsRead(subject: String, notificationId: UUID): Boolean
}

class InMemoryNotificationInboxStore : NotificationInboxStore {
    private val items = ConcurrentHashMap<String, MutableList<InboxItem>>()

    override fun append(subject: String, item: InboxItem) {
        items.computeIfAbsent(subject) { mutableListOf() }.add(item)
    }

    override fun list(subject: String): List<InboxItem> =
        items[subject]?.sortedWith(
            compareByDescending<InboxItem> { it.occurredAt }.thenByDescending { it.notificationId }
        ) ?: emptyList()

    override fun markAsRead(subject: String, notificationId: UUID): Boolean {
        val list = items[subject] ?: return false
        synchronized(list) {
            val index = list.indexOfFirst { it.notificationId == notificationId }
            if (index < 0) return false
            list[index] = list[index].copy(read = true)
            return true
        }
    }
}
