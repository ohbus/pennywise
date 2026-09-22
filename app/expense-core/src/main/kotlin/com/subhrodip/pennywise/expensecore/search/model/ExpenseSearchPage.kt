package com.subhrodip.pennywise.expensecore.search.model

/** Paginated search result with current-page currency totals. */
data class ExpenseSearchPage(val expenses: List<SearchExpense>, val nextCursor: String?, val hasMore: Boolean, val totals: List<CurrencyTotal>)
