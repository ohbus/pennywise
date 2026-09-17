package com.subhrodip.pennywise.expensecore.expenses

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [BalancePostingEntity].
 *
 * Provides operations to inspect financial postings and aggregate net balances per participant and currency.
 */
@Repository
interface BalancePostingRepository : JpaRepository<BalancePostingEntity, UUID> {
    /**
     * Retrieves all balance postings for a group.
     */
    fun findByGroupId(groupId: UUID): List<BalancePostingEntity>

    /**
     * Retrieves all balance postings for a group filtered by currency.
     */
    fun findByGroupIdAndCurrency(groupId: UUID, currency: String): List<BalancePostingEntity>

    /**
     * Retrieves all balance postings associated with a specific expense.
     */
    fun findByExpenseId(expenseId: UUID): List<BalancePostingEntity>

    /**
     * Aggregates net balance sums grouped by participant ID and currency within a group.
     *
     * @param groupId the group UUID
     * @return list of rows containing participantId, currency, and net sum
     */
    @Query(
        """
        select b.participantId, b.currency, sum(b.amountMinor)
        from BalancePostingEntity b
        where b.groupId = :groupId
        group by b.participantId, b.currency
        """
    )
    fun sumBalancesByGroup(@Param("groupId") groupId: UUID): List<Array<Any>>
}
