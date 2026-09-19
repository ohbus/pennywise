package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.categories.ExpenseCategory
import com.subhrodip.pennywise.expensecore.expenses.ExpenseRepository
import java.util.UUID
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
    override fun findSearchExpenses(groupId: UUID): List<SearchExpense> {
        val entities = expenseRepository.findByGroupIdAndDeletedFalseOrderByCreatedAtDesc(groupId)
        return entities.map { entity ->
            val category = runCatching { ExpenseCategory.fromKey(entity.category) }.getOrDefault(ExpenseCategory.OTHER)
            SearchExpense(
                expenseId = entity.expenseId.toString(),
                description = entity.description,
                currency = entity.currency,
                amountMinor = entity.amountMinor.toString(),
                category = category
            )
        }
    }
}
