package com.subhrodip.pennywise.expensecore.search

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [SearchStore] for unit and mock testing.
 */
class InMemorySearchStore : SearchStore {
    private val expensesByGroup = ConcurrentHashMap<UUID, MutableList<SearchExpense>>()

    /**
     * Adds expenses for a given group in the in-memory store.
     *
     * @param groupId the UUID of the group
     * @param expenses the expenses to store
     */
    fun saveExpenses(groupId: UUID, expenses: List<SearchExpense>) {
        expensesByGroup.computeIfAbsent(groupId) { mutableListOf() }.addAll(expenses)
    }

    /**
     * Finds active expenses for a given group in memory.
     *
     * @param groupId the UUID of the group
     * @return list of [SearchExpense] records
     */
    override fun findSearchExpenses(query: SearchQuery): List<SearchExpense> =
        expensesByGroup[query.groupId].orEmpty().asSequence()
            .filter { query.text.isBlank() || it.description.contains(query.text, ignoreCase = true) }
            .filter { query.currency == null || it.currency.equals(query.currency, ignoreCase = true) }
            .filter { query.category == null || it.category.key == query.category }
            .filter { query.afterExpenseId == null || it.expenseId > query.afterExpenseId }
            .sortedBy { it.expenseId }
            .take(query.limit + 1)
            .toList()
}
