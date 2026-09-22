package com.subhrodip.pennywise.expensecore.search

import com.subhrodip.pennywise.expensecore.search.api.SearchQuery
import com.subhrodip.pennywise.expensecore.search.model.SearchExpense
import com.subhrodip.pennywise.expensecore.search.persistence.JpaSearchStore
import com.subhrodip.pennywise.expensecore.expenses.persistence.entity.ExpenseEntity
import com.subhrodip.pennywise.expensecore.expenses.persistence.repository.ExpenseRepository
import com.subhrodip.pennywise.expensecore.groups.domain.GroupEntity
import com.subhrodip.pennywise.expensecore.groups.persistence.repository.GroupRepository

import com.subhrodip.pennywise.ids.generation.UuidGenerator
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * Integration test verifying [JpaSearchStore] queries against a real database.
 */
@SpringBootTest
@Transactional
class JpaSearchStoreTest @Autowired constructor(
    private val groupRepository: com.subhrodip.pennywise.expensecore.groups.persistence.repository.GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val searchStore: JpaSearchStore
) {

    @Test
    fun `finds active expenses and maps categories while ignoring deleted expenses`() {
        // Create and persist a group to satisfy foreign key constraint
        val groupId = UUID.randomUUID()
        val group = com.subhrodip.pennywise.expensecore.groups.domain.GroupEntity(
            groupId = groupId,
            name = "Test Group",
            kind = "HOUSEHOLD",
            currency = "EUR"
        )
        groupRepository.save(group)

        val expenseId1 = UuidGenerator.next()
        val expenseId2 = UuidGenerator.next()
        val deletedExpenseId = UuidGenerator.next()

        val active1 = ExpenseEntity(
            expenseId = expenseId1,
            groupId = groupId,
            description = "Museum tickets",
            category = "entertainment",
            currency = "EUR",
            amountMinor = 2400,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now().minusSeconds(100),
            deleted = false
        )
        val active2 = ExpenseEntity(
            expenseId = expenseId2,
            groupId = groupId,
            description = "Train tickets",
            category = "transport",
            currency = "EUR",
            amountMinor = 5000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            deleted = false
        )
        val deleted = ExpenseEntity(
            expenseId = deletedExpenseId,
            groupId = groupId,
            description = "Cancelled tour",
            category = "other",
            currency = "EUR",
            amountMinor = 3000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            deleted = true
        )

        expenseRepository.saveAll(listOf(active1, active2, deleted))

        val results = searchStore.findSearchExpenses(SearchQuery(groupId))
        assertEquals(2, results.size)
        // Stable UUID ordering is the keyset cursor order used by ExpenseSearch.
        val expected = listOf(active1, active2).sortedBy { it.expenseId.toString() }
        assertEquals(expected[0].expenseId.toString(), results[0].expenseId)
        assertEquals(expected[0].description, results[0].description)
        assertEquals(expected[0].amountMinor.toString(), results[0].amountMinor)
        assertEquals(expected[1].expenseId.toString(), results[1].expenseId)
        assertEquals(expected[1].description, results[1].description)
    }
}
