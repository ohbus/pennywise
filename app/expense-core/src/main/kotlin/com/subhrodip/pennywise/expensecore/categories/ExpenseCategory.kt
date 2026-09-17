package com.subhrodip.pennywise.expensecore.categories

enum class ExpenseCategory(val key: String, val label: String) {
    FOOD("food", "Food"),
    LODGING("lodging", "Lodging"),
    TRANSPORT("transport", "Transport"),
    ENTERTAINMENT("entertainment", "Entertainment"),
    SHOPPING("shopping", "Shopping"),
    BILLS("bills", "Bills"),
    HEALTH("health", "Health"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String): ExpenseCategory = values().firstOrNull { it.key == key.trim().lowercase() }
            ?: throw IllegalArgumentException("Unknown expense category")
    }
}

object ExpenseCategoryCatalog {
    val defaults: List<ExpenseCategory> = ExpenseCategory.values().toList()
}
