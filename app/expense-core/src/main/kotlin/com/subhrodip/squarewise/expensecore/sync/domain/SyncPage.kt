package com.subhrodip.squarewise.expensecore.sync.domain

/** Bounded synchronization page and continuation cursor. */
data class SyncPage(val changes: List<SyncChange>, val nextCursor: String?, val hasMore: Boolean)
