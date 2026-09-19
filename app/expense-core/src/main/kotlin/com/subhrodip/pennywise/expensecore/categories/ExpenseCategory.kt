package com.subhrodip.pennywise.expensecore.categories
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
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
            ?: throw ApplicationException(ErrorCode.ERR_02, "Unknown expense category")
    }
}

object ExpenseCategoryCatalog {
    val defaults: List<ExpenseCategory> = ExpenseCategory.values().toList()
}
