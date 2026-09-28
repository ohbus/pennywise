package com.subhrodip.squarewise.expensecore.sync.api

/** Cursor-paginated synchronization response. */
data class SyncPageResponse(val changes: List<SyncChangeResponse>, val nextCursor: String?, val hasMore: Boolean)
