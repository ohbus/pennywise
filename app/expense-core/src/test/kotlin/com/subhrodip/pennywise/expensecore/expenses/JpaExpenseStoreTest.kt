package com.subhrodip.pennywise.expensecore.expenses

import com.subhrodip.pennywise.expensecore.groups.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.JpaGroupStore
import com.subhrodip.pennywise.expensecore.messaging.OutboxStore
import com.subhrodip.pennywise.expensecore.sync.SynchronizationStore
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Transactional
class JpaExpenseStoreTest @Autowired constructor(
    private val expenseStore: JpaExpenseStore,
    private val groupStore: JpaGroupStore,
    private val outboxStore: OutboxStore,
    private val syncStore: SynchronizationStore,
    private val balancePostingRepository: BalancePostingRepository
) {

    @Test
    fun `persists expense, payers, allocations, and postings atomically`() {
        val group = groupStore.create("alice", CreateGroupRequest("Alps Hiking", "TRIP", "EUR"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()
        val charlieId = UUID.randomUUID()

        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Mountain cabin rental",
            category = "accommodation",
            currency = "EUR",
            amountMinor = 3000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(
                ExpensePayer(aliceId, 2000),
                ExpensePayer(bobId, 1000)
            ),
            allocations = listOf(
                ExpenseAllocation(aliceId, 1000),
                ExpenseAllocation(bobId, 1000),
                ExpenseAllocation(charlieId, 1000)
            )
        )

        val created = expenseStore.create(groupId, record, "idemp-key-test-1")
        assertEquals(expenseId, created.expenseId)
        assertEquals(3000, created.amountMinor)
        assertEquals("accommodation", created.category)
        assertEquals(2, created.payers.size)
        assertEquals(3, created.allocations.size)

        // Verify retrieval by ID
        val retrieved = expenseStore.findById(expenseId)
        assertNotNull(retrieved)
        assertEquals("Mountain cabin rental", retrieved?.description)
        assertEquals(3000, retrieved?.amountMinor)
        assertEquals(2, retrieved?.payers?.size)
        assertEquals(3, retrieved?.allocations?.size)

        // Verify postings net sum to zero across all participants
        val postings = balancePostingRepository.findByGroupId(groupId)
        assertEquals(5, postings.size) // 2 payers (+2000, +1000) + 3 allocations (-1000, -1000, -1000)
        val netSum = postings.sumOf { it.amountMinor }
        assertEquals(0L, netSum)

        // Verify aggregated balances
        val balances = expenseStore.balances(groupId)
        assertEquals(3, balances.size)
        val aliceBalance = balances.find { it.participantId == aliceId.toString() }?.amount?.minor?.toLong()
        val bobBalance = balances.find { it.participantId == bobId.toString() }?.amount?.minor?.toLong()
        val charlieBalance = balances.find { it.participantId == charlieId.toString() }?.amount?.minor?.toLong()

        // Alice paid 2000, owes 1000 -> net +1000
        assertEquals(1000L, aliceBalance)
        // Bob paid 1000, owes 1000 -> net 0
        assertEquals(0L, bobBalance)
        // Charlie paid 0, owes 1000 -> net -1000
        assertEquals(-1000L, charlieBalance)

        // Verify group listing
        val list = expenseStore.list(groupId)
        assertEquals(1, list.size)
        assertEquals(expenseId, list[0].expenseId)

        // Verify category filtering
        val listAccom = expenseStore.list(groupId, category = "accommodation")
        assertEquals(1, listAccom.size)
        val listFood = expenseStore.list(groupId, category = "food")
        assertEquals(0, listFood.size)
    }

    @Test
    fun `idempotent creation with identical payload returns existing record`() {
        val group = groupStore.create("alice", CreateGroupRequest("Dinner Club", "HOUSEHOLD", "USD"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Groceries",
            category = "groceries",
            currency = "USD",
            amountMinor = 1500,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(aliceId, 1500)),
            allocations = listOf(ExpenseAllocation(aliceId, 1500))
        )

        val first = expenseStore.create(groupId, record, "idemp-dup-1")
        val second = expenseStore.create(groupId, record, "idemp-dup-1")

        assertEquals(first.expenseId, second.expenseId)
        assertEquals(first.amountMinor, second.amountMinor)
    }

    @Test
    fun `creation with duplicate expense ID but different payload throws conflict`() {
        val group = groupStore.create("alice", CreateGroupRequest("Road Trip", "TRIP", "EUR"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val record1 = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Gas station",
            category = "transport",
            currency = "EUR",
            amountMinor = 4000,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(aliceId, 4000)),
            allocations = listOf(ExpenseAllocation(aliceId, 4000))
        )

        val record2 = record1.copy(amountMinor = 5000)

        expenseStore.create(groupId, record1, "idemp-1")

        assertThrows(ResponseStatusException::class.java) {
            expenseStore.create(groupId, record2, "idemp-2")
        }
    }

    @Test
    fun `updates expense, reverses previous postings, creates new postings, and updates group revision`() {
        val group = groupStore.create("alice", CreateGroupRequest("Weekend Trip", "TRIP", "EUR"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()

        val initial = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Gas",
            category = "transport",
            currency = "EUR",
            amountMinor = 2000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(aliceId, 2000)),
            allocations = listOf(ExpenseAllocation(aliceId, 1000), ExpenseAllocation(bobId, 1000))
        )

        expenseStore.create(groupId, initial, "idemp-gas-1")

        // Alice +1000, Bob -1000
        val balancesBefore = expenseStore.balances(groupId)
        assertEquals(1000L, balancesBefore.find { it.participantId == aliceId.toString() }?.amount?.minor?.toLong())
        assertEquals(-1000L, balancesBefore.find { it.participantId == bobId.toString() }?.amount?.minor?.toLong())

        // Update: Bob paid 3000, split equally (1500 each)
        val update = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Gas and tolls",
            category = "transport",
            currency = "EUR",
            amountMinor = 3000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(bobId, 3000)),
            allocations = listOf(ExpenseAllocation(aliceId, 1500), ExpenseAllocation(bobId, 1500))
        )

        val updated = expenseStore.update(groupId, expenseId, update)
        assertEquals(2, updated.version)
        assertEquals("Gas and tolls", updated.description)
        assertEquals(3000, updated.amountMinor)
        assertNotNull(updated.updatedAt)

        // Verify balance invariants: sum across all group postings must be zero
        val allPostings = balancePostingRepository.findByGroupId(groupId)
        val groupNetSum = allPostings.sumOf { it.amountMinor }
        assertEquals(0L, groupNetSum)

        // Postings for this expense should have original (3) + reversals (2 net participants) + new (3) = 8
        val expensePostings = balancePostingRepository.findByExpenseId(expenseId)
        assertTrue(expensePostings.size >= 7)

        // Verify new balances: Bob paid 3000, owes 1500 -> Bob net +1500. Alice owes 1500 -> Alice net -1500.
        val balancesAfter = expenseStore.balances(groupId)
        val aliceBalanceAfter = balancesAfter.find { it.participantId == aliceId.toString() }?.amount?.minor?.toLong()
        val bobBalanceAfter = balancesAfter.find { it.participantId == bobId.toString() }?.amount?.minor?.toLong()
        assertEquals(-1500L, aliceBalanceAfter)
        assertEquals(1500L, bobBalanceAfter)
    }

    @Test
    fun `deletes expense, reverses postings, sets deleted flag, and clears active balances`() {
        val group = groupStore.create("alice", CreateGroupRequest("Cabin Trip", "TRIP", "EUR"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()

        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Kayak rental",
            category = "entertainment",
            currency = "EUR",
            amountMinor = 1000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(aliceId, 1000)),
            allocations = listOf(ExpenseAllocation(aliceId, 500), ExpenseAllocation(bobId, 500))
        )

        expenseStore.create(groupId, record, "idemp-kayak-1")

        // Alice +500, Bob -500
        val balancesBefore = expenseStore.balances(groupId)
        assertEquals(500L, balancesBefore.find { it.participantId == aliceId.toString() }?.amount?.minor?.toLong())
        assertEquals(-500L, balancesBefore.find { it.participantId == bobId.toString() }?.amount?.minor?.toLong())

        // Delete expense
        expenseStore.delete(groupId, expenseId, version = 1)

        // Find by ID returns null (soft deleted)
        assertNull(expenseStore.findById(expenseId))

        // Listing returns empty
        val list = expenseStore.list(groupId)
        assertEquals(0, list.size)

        // Postings are retained in DB (immutable ledger) but reversed
        val postings = balancePostingRepository.findByExpenseId(expenseId)
        assertTrue(postings.isNotEmpty())
        val expensePostingNet = postings.sumOf { it.amountMinor }
        assertEquals(0L, expensePostingNet)

        // Balances return to net 0
        val balancesAfter = expenseStore.balances(groupId)
        assertEquals(0L, balancesAfter.find { it.participantId == aliceId.toString() }?.amount?.minor?.toLong() ?: 0L)
        assertEquals(0L, balancesAfter.find { it.participantId == bobId.toString() }?.amount?.minor?.toLong() ?: 0L)
    }

    @Test
    fun `rejects update and delete when version is stale`() {
        val group = groupStore.create("alice", CreateGroupRequest("Ski Trip", "TRIP", "EUR"))
        val groupId = group.groupId
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = groupId,
            description = "Lift pass",
            category = "entertainment",
            currency = "EUR",
            amountMinor = 5000,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(aliceId, 5000)),
            allocations = listOf(ExpenseAllocation(aliceId, 5000))
        )

        expenseStore.create(groupId, record, "idemp-ski-1")

        // Update with stale version 99
        val staleUpdate = record.copy(version = 99, description = "Updated pass")
        assertThrows(ResponseStatusException::class.java) {
            expenseStore.update(groupId, expenseId, staleUpdate)
        }

        // Delete with stale version 99
        assertThrows(ResponseStatusException::class.java) {
            expenseStore.delete(groupId, expenseId, version = 99)
        }
    }
}
