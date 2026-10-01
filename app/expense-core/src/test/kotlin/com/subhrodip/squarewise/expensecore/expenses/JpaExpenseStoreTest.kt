package com.subhrodip.squarewise.expensecore.expenses
import com.subhrodip.squarewise.expensecore.expenses.domain.ExpenseAllocation
import com.subhrodip.squarewise.expensecore.expenses.domain.ExpensePayer
import com.subhrodip.squarewise.expensecore.expenses.domain.ExpenseRecord
import com.subhrodip.squarewise.expensecore.expenses.persistence.repository.BalancePostingRepository
import com.subhrodip.squarewise.expensecore.expenses.persistence.store.JpaExpenseStore
import com.subhrodip.squarewise.expensecore.groups.api.CreateGroupRequest
import com.subhrodip.squarewise.expensecore.groups.persistence.store.JpaGroupStore
import com.subhrodip.squarewise.expensecore.groups.persistence.repository.GroupMembershipRepository
import com.subhrodip.squarewise.expensecore.groups.persistence.repository.GroupRepository
import com.subhrodip.squarewise.expensecore.messaging.outbox.persistence.OutboxStore

import com.subhrodip.squarewise.expensecore.sync.persistence.SynchronizationStore
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
import com.subhrodip.squarewise.errors.domain.ApplicationException
import com.subhrodip.squarewise.errors.domain.ErrorCode

@SpringBootTest
@Transactional
class JpaExpenseStoreTest @Autowired constructor(
    private val expenseStore: JpaExpenseStore,
    private val groupStore: JpaGroupStore,
    private val groupRepository: GroupRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val outboxStore: OutboxStore,
    private val syncStore: SynchronizationStore,
    private val balancePostingRepository: BalancePostingRepository
) {

    @Test
    fun `actor-scoped creation accepts active membership identifiers`() {
        val group = groupStore.create("alice", CreateGroupRequest("Member trip", "TRIP", "EUR"))
        val membershipId = membershipRepository.findByGroupId(group.groupId).single().membershipId
        val expenseId = UUID.randomUUID()
        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = group.groupId,
            description = "Shared hotel",
            category = "lodging",
            currency = "EUR",
            amountMinor = 1000,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(membershipId, 1000)),
            allocations = listOf(ExpenseAllocation(membershipId, 1000))
        )

        val created = expenseStore.create(group.groupId, record, "actor-create-key", "alice")

        assertEquals(expenseId, created.expenseId)
        assertEquals(2, balancePostingRepository.findByExpenseId(expenseId).size)
    }

    @Test
    fun `actor-scoped creation rejects duplicate or inactive participants before mutation`() {
        val group = groupStore.create("alice", CreateGroupRequest("Validated trip", "TRIP", "EUR"))
        val activeMembershipId = membershipRepository.findByGroupId(group.groupId).single().membershipId
        val initialRevision = groupRepository.findById(group.groupId).orElseThrow().revision

        val duplicateId = UUID.randomUUID()
        val duplicateParticipant = ExpenseRecord(
            expenseId = duplicateId,
            groupId = group.groupId,
            description = "Duplicate payer",
            category = "food",
            currency = "EUR",
            amountMinor = 1000,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(activeMembershipId, 500), ExpensePayer(activeMembershipId, 500)),
            allocations = listOf(ExpenseAllocation(activeMembershipId, 1000))
        )
        val duplicateError = assertThrows(ApplicationException::class.java) {
            expenseStore.create(group.groupId, duplicateParticipant, "duplicate-key", "alice")
        }
        assertEquals(ErrorCode.ERR_02, duplicateError.errorCode)

        val inactiveId = UUID.randomUUID()
        val inactiveParticipant = duplicateParticipant.copy(
            expenseId = UUID.randomUUID(),
            payers = listOf(ExpensePayer(activeMembershipId, 1000)),
            allocations = listOf(ExpenseAllocation(inactiveId, 1000))
        )
        val inactiveError = assertThrows(ApplicationException::class.java) {
            expenseStore.create(group.groupId, inactiveParticipant, "inactive-key", "alice")
        }
        assertEquals(ErrorCode.ERR_05, inactiveError.errorCode)

        assertEquals(initialRevision, groupRepository.findById(group.groupId).orElseThrow().revision)
        assertNull(expenseStore.findById(duplicateId))
        assertTrue(balancePostingRepository.findByGroupId(group.groupId).isEmpty())
    }

    @Test
    fun `actor-scoped update and delete reject non-members without mutation`() {
        val group = groupStore.create("alice", CreateGroupRequest("Owned trip", "TRIP", "EUR"))
        val membershipId = membershipRepository.findByGroupId(group.groupId).single().membershipId
        val expenseId = UUID.randomUUID()
        val record = ExpenseRecord(
            expenseId = expenseId,
            groupId = group.groupId,
            description = "Train tickets",
            category = "travel",
            currency = "EUR",
            amountMinor = 1200,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(membershipId, 1200)),
            allocations = listOf(ExpenseAllocation(membershipId, 1200))
        )
        expenseStore.create(group.groupId, record, "member-create-key", "alice")
        val revisionBefore = groupRepository.findById(group.groupId).orElseThrow().revision

        val updateError = assertThrows(ApplicationException::class.java) {
            expenseStore.update(group.groupId, expenseId, record.copy(description = "Changed"), "bob")
        }
        assertEquals(ErrorCode.ERR_05, updateError.errorCode)

        val deleteError = assertThrows(ApplicationException::class.java) {
            expenseStore.delete(group.groupId, expenseId, version = 1, actorSubject = "bob")
        }
        assertEquals(ErrorCode.ERR_05, deleteError.errorCode)

        val unchanged = expenseStore.findById(expenseId)
        assertEquals("Train tickets", unchanged?.description)
        assertEquals(1, unchanged?.version)
        assertEquals(revisionBefore, groupRepository.findById(group.groupId).orElseThrow().revision)
        assertEquals(2, balancePostingRepository.findByExpenseId(expenseId).size)
    }

    @Test
    fun `archived groups reject create update and delete without financial mutation`() {
        val group = groupStore.create("alice", CreateGroupRequest("Archived trip", "TRIP", "EUR"))
        val membershipId = membershipRepository.findByGroupId(group.groupId).single().membershipId
        val expenseId = UUID.randomUUID()
        val existing = ExpenseRecord(
            expenseId = expenseId,
            groupId = group.groupId,
            description = "Existing fare",
            category = "travel",
            currency = "EUR",
            amountMinor = 800,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(membershipId, 800)),
            allocations = listOf(ExpenseAllocation(membershipId, 800))
        )
        expenseStore.create(group.groupId, existing, "archive-create-key", "alice")
        groupStore.archive(group.groupId, "alice")
        val archivedRevision = groupRepository.findById(group.groupId).orElseThrow().revision

        val newExpense = existing.copy(expenseId = UUID.randomUUID(), description = "Rejected fare")
        val createError = assertThrows(ApplicationException::class.java) {
            expenseStore.create(group.groupId, newExpense, "archive-rejected-key", "alice")
        }
        assertEquals(ErrorCode.ERR_06, createError.errorCode)

        val updateError = assertThrows(ApplicationException::class.java) {
            expenseStore.update(group.groupId, expenseId, existing.copy(description = "Rejected update"), "alice")
        }
        assertEquals(ErrorCode.ERR_06, updateError.errorCode)

        val deleteError = assertThrows(ApplicationException::class.java) {
            expenseStore.delete(group.groupId, expenseId, version = 1, actorSubject = "alice")
        }
        assertEquals(ErrorCode.ERR_06, deleteError.errorCode)

        assertEquals(archivedRevision, groupRepository.findById(group.groupId).orElseThrow().revision)
        assertEquals("Existing fare", expenseStore.findById(expenseId)?.description)
        assertEquals(2, balancePostingRepository.findByExpenseId(expenseId).size)
        assertNull(expenseStore.findById(newExpense.expenseId))
    }

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

        val err = assertThrows(ApplicationException::class.java) {
            expenseStore.create(groupId, record2, "idemp-2")
        }
        assertEquals(ErrorCode.ERR_06, err.errorCode)
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
    fun `updates existing payer and allocation rows in place`() {
        val group = groupStore.create("alice", CreateGroupRequest("In-place update", "TRIP", "EUR"))
        val expenseId = UUID.randomUUID()
        val participantId = UUID.randomUUID()
        val initial = ExpenseRecord(
            expenseId = expenseId,
            groupId = group.groupId,
            description = "Initial charge",
            category = "travel",
            currency = "EUR",
            amountMinor = 2000,
            version = 1,
            allocationMode = "EXACT",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(participantId, 2000)),
            allocations = listOf(ExpenseAllocation(participantId, 2000))
        )
        expenseStore.create(group.groupId, initial, "idemp-in-place")

        val updated = expenseStore.update(
            group.groupId,
            expenseId,
            initial.copy(
                description = "Updated charge",
                amountMinor = 3000,
                payers = listOf(ExpensePayer(participantId, 3000)),
                allocations = listOf(ExpenseAllocation(participantId, 3000))
            )
        )

        assertEquals(2, updated.version)
        assertEquals("Updated charge", updated.description)
        assertEquals(listOf(ExpensePayer(participantId, 3000)), updated.payers)
        assertEquals(listOf(ExpenseAllocation(participantId, 3000)), updated.allocations)
        assertEquals(0L, balancePostingRepository.findByExpenseId(expenseId).sumOf { it.amountMinor })
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
        val updateErr = assertThrows(ApplicationException::class.java) {
            expenseStore.update(groupId, expenseId, staleUpdate)
        }
        assertEquals(ErrorCode.ERR_06, updateErr.errorCode)

        // Delete with stale version 99
        val deleteErr = assertThrows(ApplicationException::class.java) {
            expenseStore.delete(groupId, expenseId, version = 99)
        }
        assertEquals(ErrorCode.ERR_06, deleteErr.errorCode)
    }
}
