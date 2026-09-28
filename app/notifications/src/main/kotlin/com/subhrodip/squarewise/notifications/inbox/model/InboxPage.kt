package com.subhrodip.squarewise.notifications.inbox.model

/** Cursor-paginated inbox response. */
data class InboxPage(val items: List<InboxItem>, val nextCursor: String? = null)
