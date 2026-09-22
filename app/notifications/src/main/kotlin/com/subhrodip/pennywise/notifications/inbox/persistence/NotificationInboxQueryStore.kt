package com.subhrodip.pennywise.notifications.inbox.persistence

import com.subhrodip.pennywise.notifications.inbox.model.InboxItem
/** Reader-side persistence port for historical inbox items. */
interface NotificationInboxQueryStore {
    /** Lists inbox items belonging to a subject. */
    fun list(subject: String): List<InboxItem>
}
