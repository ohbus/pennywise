package com.subhrodip.pennywise.notifications.inbox.persistence

import com.subhrodip.pennywise.notifications.inbox.model.InboxItem
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Thread-safe in-memory inbox adapter used by tests and local development. */
class InMemoryNotificationInboxStore : NotificationInboxStore {
    private val items = ConcurrentHashMap<String, MutableList<InboxItem>>()
    override fun append(subject: String, item: InboxItem) { items.computeIfAbsent(subject) { mutableListOf() }.add(item) }
    override fun list(subject: String): List<InboxItem> = items[subject]?.sortedWith(compareByDescending<InboxItem> { it.occurredAt }.thenByDescending { it.notificationId }) ?: emptyList()
    override fun markAsRead(subject: String, notificationId: UUID): Boolean {
        val list = items[subject] ?: return false
        synchronized(list) { val index = list.indexOfFirst { it.notificationId == notificationId }; if (index < 0) return false; list[index] = list[index].copy(read = true); return true }
    }
}
