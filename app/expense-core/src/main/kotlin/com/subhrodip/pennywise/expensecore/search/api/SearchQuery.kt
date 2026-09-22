package com.subhrodip.pennywise.expensecore.search.api

import com.subhrodip.pennywise.expensecore.search.model.ExpenseSearch
import java.util.UUID

/** Normalized bounded search request passed to a read adapter. */
data class SearchQuery(
    val groupId: UUID,
    val text: String = "",
    val currency: String? = null,
    val category: String? = null,
    val afterExpenseId: String? = null,
    val limit: Int = ExpenseSearch.DEFAULT_LIMIT
)
