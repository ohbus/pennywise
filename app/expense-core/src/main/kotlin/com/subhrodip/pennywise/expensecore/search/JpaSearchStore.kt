package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.categories.ExpenseCategory
import com.subhrodip.pennywise.expensecore.expenses.ExpenseRepository
import org.springframework.data.domain.PageRequest
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Spring Data JPA implementation of [SearchStore] backed by PostgreSQL.
 *
 * Retrieves active expenses for a group and maps them into domain [SearchExpense] models.
 */
@Service
@Primary
class JpaSearchStore(
    private val expenseRepository: ExpenseRepository
) : SearchStore {

    /**
     * Finds active expenses for a group in persistent storage.
     *
     * @param groupId the UUID of the group
     * @return list of [SearchExpense] domain records
     */
    @Transactional(readOnly = true)
    override fun findSearchExpenses(query: SearchQuery): List<SearchExpense> {
        require(query.limit in 1..ExpenseSearch.MAX_EXPORT_ROWS)
        return expenseRepository.searchProjection(
            query.groupId, query.text, query.currency, query.category,
            query.afterExpenseId, PageRequest.of(0, query.limit + 1)
        ).map { projection ->
            val category = runCatching { ExpenseCategory.fromKey(projection.getCategory()) }.getOrDefault(ExpenseCategory.OTHER)
            SearchExpense(
                expenseId = projection.getExpenseId(),
                description = projection.getDescription(),
                currency = projection.getCurrency(),
                amountMinor = projection.getAmountMinor().toString(),
                category = category
            )
        }
    }
}
