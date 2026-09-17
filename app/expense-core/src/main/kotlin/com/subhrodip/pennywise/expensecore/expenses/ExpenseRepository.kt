package com.subhrodip.pennywise.expensecore.expenses

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [ExpenseEntity].
 *
 * Provides query methods for retrieving non-deleted expenses by group and category.
 */
@Repository
interface ExpenseRepository : JpaRepository<ExpenseEntity, UUID> {
    /**
     * Retrieves all expenses in a group ordered by creation date descending.
     */
    fun findByGroupIdOrderByCreatedAtDesc(groupId: UUID): List<ExpenseEntity>

    /**
     * Retrieves expenses in a group filtered by category ordered by creation date descending.
     */
    fun findByGroupIdAndCategoryOrderByCreatedAtDesc(groupId: UUID, category: String): List<ExpenseEntity>

    /**
     * Retrieves active (non-deleted) expenses in a group ordered by creation date descending.
     */
    fun findByGroupIdAndDeletedFalseOrderByCreatedAtDesc(groupId: UUID): List<ExpenseEntity>

    /**
     * Retrieves active (non-deleted) expenses in a group filtered by category ordered by creation date descending.
     */
    fun findByGroupIdAndCategoryAndDeletedFalseOrderByCreatedAtDesc(groupId: UUID, category: String): List<ExpenseEntity>

    /**
     * Finds an expense by ID and group ID.
     */
    fun findByExpenseIdAndGroupId(expenseId: UUID, groupId: UUID): ExpenseEntity?
}
