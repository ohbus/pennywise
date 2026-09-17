package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.categories.ExpenseCategory
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Base64

data class SearchExpense(val expenseId: String, val description: String, val currency: String, val amountMinor: String, val category: ExpenseCategory = ExpenseCategory.OTHER)

data class CurrencyTotal(val currency: String, val amountMinor: String)

data class ExpenseSearchPage(
    val expenses: List<SearchExpense>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val totals: List<CurrencyTotal>
)

class ExpenseSearch {
    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 1000
        const val MAX_EXPORT_ROWS = 10_000
    }

    fun filter(expenses: Iterable<SearchExpense>, query: String, limit: Int = DEFAULT_LIMIT): List<SearchExpense> =
        page(expenses, query = query, limit = limit).expenses

    fun page(
        expenses: Iterable<SearchExpense>,
        query: String = "",
        currency: String? = null,
        category: String? = null,
        cursor: String? = null,
        limit: Int = DEFAULT_LIMIT
    ): ExpenseSearchPage {
        require(limit in 1..MAX_LIMIT)
        val normalized = query.trim().lowercase()
        val normalizedCurrency = currency?.trim()?.uppercase()?.also { require(it.matches(Regex("[A-Z]{3}"))) }
        val normalizedCategory = category?.let(ExpenseCategory::fromKey)
        val startAfter = cursor?.let(::decodeCursor)
        val matches = expenses.asSequence()
            .filter { normalized.isEmpty() || it.description.lowercase().contains(normalized) }
            .filter { normalizedCurrency == null || it.currency.uppercase() == normalizedCurrency }
            .filter { normalizedCategory == null || it.category == normalizedCategory }
            .sortedBy { it.expenseId }
            .filter { startAfter == null || it.expenseId > startAfter }
            .toList()
        val page = matches.take(limit)
        val hasMore = matches.size > limit
        return ExpenseSearchPage(
            expenses = page,
            nextCursor = if (hasMore) encodeCursor(page.last().expenseId) else null,
            hasMore = hasMore,
            totals = totals(page)
        )
    }

    fun csv(
        expenses: Iterable<SearchExpense>,
        query: String = "",
        currency: String? = null,
        category: String? = null,
        maxRows: Int = MAX_EXPORT_ROWS
    ): String {
        require(maxRows in 1..MAX_EXPORT_ROWS)
        val page = page(expenses, query, currency, category, limit = minOf(maxRows, MAX_LIMIT))
        require(!page.hasMore) { "Export exceeds the maximum row limit" }
        return buildString {
            appendLine("expenseId,description,currency,amountMinor,category")
            page.expenses.forEach { expense ->
                appendLine(listOf(expense.expenseId, expense.description, expense.currency, expense.amountMinor, expense.category.key)
                    .joinToString(",", transform = ::csvCell))
            }
        }
    }

    private fun totals(expenses: List<SearchExpense>): List<CurrencyTotal> = expenses.groupBy { it.currency.uppercase() }
        .toSortedMap().map { (currency, values) ->
            CurrencyTotal(currency, values.fold(BigInteger.ZERO) { total, expense -> total + expense.amountMinor.toBigInteger() }.toString())
        }

    fun csvCell(value: String): String {
        val safe = if (value.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${safe.replace("\"", "\"\"")}\"" else safe
    }

    private fun encodeCursor(expenseId: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(expenseId.toByteArray(StandardCharsets.UTF_8))

    private fun decodeCursor(cursor: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).also { require(it.isNotBlank()) }
    }.getOrElse { throw IllegalArgumentException("Invalid search cursor") }
}
