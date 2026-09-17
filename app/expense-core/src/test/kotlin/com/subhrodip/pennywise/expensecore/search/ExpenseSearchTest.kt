package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.categories.ExpenseCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExpenseSearchTest {
    @Test
    fun `filters case insensitively with stable bounded results and escapes formulas`() {
        val search = ExpenseSearch()
        val data = listOf(SearchExpense("2", "Dinner", "EUR", "1000"), SearchExpense("1", "dinner taxi", "EUR", "2000"))
        assertEquals(listOf("1", "2"), search.filter(data, "DINNER").map { it.expenseId })
        assertEquals("'=SUM(A1)", search.csvCell("=SUM(A1)"))
    }

    @Test
    fun `pages with an opaque stable cursor and per currency totals`() {
        val search = ExpenseSearch()
        val data = listOf(
            SearchExpense("3", "Hotel", "USD", "500"),
            SearchExpense("1", "Train", "EUR", "200"),
            SearchExpense("2", "Meal", "EUR", "300")
        )
        val first = search.page(data, limit = 2)
        assertEquals(listOf("1", "2"), first.expenses.map { it.expenseId })
        assertEquals(listOf("500"), first.totals.map { it.amountMinor })
        val second = search.page(data, cursor = first.nextCursor, limit = 2)
        assertEquals(listOf("3"), second.expenses.map { it.expenseId })
        assertEquals(null, second.nextCursor)
    }

    @Test
    fun `filters currency and quotes csv fields while bounding export`() {
        val search = ExpenseSearch()
        val data = listOf(SearchExpense("1", "Lunch, team", "eur", "200"), SearchExpense("2", "Dinner", "USD", "100"))
        assertEquals(listOf("1"), search.page(data, currency = "EUR").expenses.map { it.expenseId })
        assertEquals("expenseId,description,currency,amountMinor,category\n1,\"Lunch, team\",eur,200,other\n", search.csv(data, currency = "EUR"))
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) { search.csv(data, maxRows = 1) }
    }

    @Test
    fun `filters by category and defaults legacy records to other`() {
        val search = ExpenseSearch()
        val data = listOf(
            SearchExpense("1", "Dinner", "EUR", "100", ExpenseCategory.FOOD),
            SearchExpense("2", "Hotel", "EUR", "200", ExpenseCategory.LODGING),
            SearchExpense("3", "Misc", "EUR", "300")
        )
        assertEquals(listOf("1"), search.page(data, category = "food").expenses.map { it.expenseId })
        assertEquals(listOf("3"), search.page(data, category = "OTHER").expenses.map { it.expenseId })
    }

    @Test
    fun `rejects unknown category`() {
        assertThrows(IllegalArgumentException::class.java) { ExpenseCategory.fromKey("travel") }
    }
}
