package com.subhrodip.pennywise.expensecore.search

import java.util.UUID

/**
 * Domain port for durable search and retrieval of expenses within an authorized group.
 */
interface SearchStore {
    /**
     * Finds all active (non-deleted) expenses belonging to the specified group.
     *
     * @param groupId the UUID of the group
     * @return sequence or list of domain [SearchExpense] records
     */
    fun findSearchExpenses(query: SearchQuery): List<SearchExpense>
}

/** Normalized bounded search request passed to the read adapter. */
data class SearchQuery(
    /** Authorized group identifier. */
    val groupId: UUID,
    /** Case-insensitive description fragment. */
    val text: String = "",
    /** Optional uppercase currency filter. */
    val currency: String? = null,
    /** Optional category key filter. */
    val category: String? = null,
    /** Last UUID cursor, or null for the first page. */
    val afterExpenseId: String? = null,
    /** Requested page size; one extra row is fetched for `hasMore`. */
    val limit: Int = ExpenseSearch.DEFAULT_LIMIT
)
