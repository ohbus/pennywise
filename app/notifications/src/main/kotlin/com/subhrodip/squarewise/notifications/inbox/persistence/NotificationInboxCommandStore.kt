package com.subhrodip.squarewise.notifications.inbox.persistence

import com.subhrodip.squarewise.notifications.inbox.model.InboxItem
import java.util.UUID

/** Writer-side persistence port for notification inbox mutations. */
interface NotificationInboxCommandStore {
    /** Appends an item for a subject. */
    fun append(subject: String, item: InboxItem)
    /** Marks a subject-owned notification as read. */
    fun markAsRead(subject: String, notificationId: UUID): Boolean
}
