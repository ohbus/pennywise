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
    fun findSearchExpenses(groupId: UUID): List<SearchExpense>
}
