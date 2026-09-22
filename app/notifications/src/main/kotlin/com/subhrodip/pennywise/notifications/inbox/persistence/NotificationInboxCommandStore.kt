package com.subhrodip.pennywise.notifications.inbox.persistence

import com.subhrodip.pennywise.notifications.inbox.model.InboxItem
import java.util.UUID

/** Writer-side persistence port for notification inbox mutations. */
interface NotificationInboxCommandStore {
    /** Appends an item for a subject. */
    fun append(subject: String, item: InboxItem)
    /** Marks a subject-owned notification as read. */
    fun markAsRead(subject: String, notificationId: UUID): Boolean
}
