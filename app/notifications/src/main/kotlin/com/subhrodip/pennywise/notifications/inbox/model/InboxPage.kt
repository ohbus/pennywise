package com.subhrodip.pennywise.notifications.inbox.model

/** Cursor-paginated inbox response. */
data class InboxPage(val items: List<InboxItem>, val nextCursor: String? = null)
