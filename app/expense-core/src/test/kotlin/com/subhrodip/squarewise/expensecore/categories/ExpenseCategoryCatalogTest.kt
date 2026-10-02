package com.subhrodip.squarewise.expensecore.categories

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies the API category catalog exposes every supported category in enum order. */
class ExpenseCategoryCatalogTest {
    @Test
    fun `defaults contain every supported expense category`() {
        assertEquals(ExpenseCategory.entries, ExpenseCategoryCatalog.defaults)
        assertEquals(ExpenseCategory.entries.map { it.key }, ExpenseCategoryCatalog.defaults.map { it.key })
    }
}
