package com.subhrodip.squarewise.expensecore.search.model

import com.subhrodip.squarewise.expensecore.categories.ExpenseCategory

/** Expense projection used by authorized search and export operations. */
data class SearchExpense(val expenseId: String, val description: String, val currency: String, val amountMinor: String, val category: ExpenseCategory = ExpenseCategory.OTHER)
