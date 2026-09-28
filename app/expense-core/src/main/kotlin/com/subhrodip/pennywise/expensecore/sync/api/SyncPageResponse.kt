package com.subhrodip.pennywise.expensecore.sync.api

/** Cursor-paginated synchronization response. */
data class SyncPageResponse(val changes: List<SyncChangeResponse>, val nextCursor: String?, val hasMore: Boolean)
