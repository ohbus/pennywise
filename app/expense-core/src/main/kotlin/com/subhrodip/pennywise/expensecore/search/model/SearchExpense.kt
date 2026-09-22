package com.subhrodip.pennywise.expensecore.search.model

import com.subhrodip.pennywise.expensecore.categories.ExpenseCategory

/** Expense projection used by authorized search and export operations. */
data class SearchExpense(val expenseId: String, val description: String, val currency: String, val amountMinor: String, val category: ExpenseCategory = ExpenseCategory.OTHER)
