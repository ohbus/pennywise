package com.subhrodip.pennywise.expensecore.categories

/** Stable catalog of categories supported by the expense API. */
object ExpenseCategoryCatalog {
    val defaults: List<ExpenseCategory> = ExpenseCategory.values().toList()
}
