package com.subhrodip.pennywise.expensecore.expenses

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.http.HttpStatus
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import com.subhrodip.pennywise.ids.UuidGenerator

/**
 * Persistence abstraction for expense records and group balances.
 */
interface ExpenseStore {
    /**
     * Creates an expense record and writes its corresponding ledger postings.
     *
     * @param groupId group UUID
     * @param expense expense details to persist
     * @param idempotencyKey idempotency key provided with the request
     * @return persisted expense record
     */
    fun create(
        groupId: UUID,
        expense: ExpenseRecord,
        idempotencyKey: String,
        actorSubject: String? = null
    ): ExpenseRecord

    /**
     * Updates an existing expense record, updating double-entry ledger postings with net reversals.
     *
     * @param groupId group UUID
     * @param expenseId expense UUID
     * @param update updated expense details
     * @return updated expense record
     */
    fun update(groupId: UUID, expenseId: UUID, update: ExpenseRecord, actorSubject: String? = null): ExpenseRecord

    /**
     * Soft-deletes an expense record, posting double-entry ledger reversals.
     *
     * @param groupId group UUID
     * @param expenseId expense UUID
     * @param version optional optimistic concurrency version to check
     */
    fun delete(groupId: UUID, expenseId: UUID, version: Long?, actorSubject: String? = null)

    /**
     * Finds an active (non-deleted) expense record by ID.
     *
     * @param expenseId expense UUID
     * @return matching active expense record, or null if not found or deleted
     */
    fun findById(expenseId: UUID): ExpenseRecord?

    /**
     * Lists active expenses in a group with optional category filter and pagination limit.
     *
     * @param groupId group UUID
     * @param category optional category name filter
     * @param cursor optional pagination cursor
     * @param limit maximum number of records to return
     * @return list of matching active expense records
     */
    fun list(groupId: UUID, category: String? = null, cursor: String? = null, limit: Int = 50): List<ExpenseRecord>

    /**
     * Computes net balances per participant and currency across all postings in a group.
     *
     * @param groupId group UUID
     * @return list of group balance items
     */
    fun balances(groupId: UUID): List<GroupBalanceItem>
}

/**
 * In-memory thread-safe implementation of [ExpenseStore] used for testing.
 */
class InMemoryExpenseStore : ExpenseStore {
    private val expenses = ConcurrentHashMap<UUID, ExpenseRecord>()
    private val postings = ConcurrentHashMap<UUID, MutableList<BalancePostingEntity>>()

    @Synchronized
    override fun create(
        groupId: UUID,
        expense: ExpenseRecord,
        idempotencyKey: String,
        actorSubject: String?
    ): ExpenseRecord {
        val existing = expenses[expense.expenseId]
        if (existing != null) {
            if (existing.amountMinor == expense.amountMinor &&
                existing.currency == expense.currency &&
                existing.description == expense.description &&
                existing.payers == expense.payers &&
                existing.allocations == expense.allocations
            ) {
                return existing
            }
            throw ApplicationException(ErrorCode.ERR_06, "Expense already exists with different payload")
        }
        expenses[expense.expenseId] = expense
        val groupPostings = postings.computeIfAbsent(groupId) { mutableListOf() }
        expense.payers.forEach { payer ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expense.expenseId,
                    participantId = payer.participantId,
                    currency = expense.currency,
                    amountMinor = payer.amountMinor,
                    createdAt = expense.createdAt
                )
            )
        }
        expense.allocations.forEach { alloc ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expense.expenseId,
                    participantId = alloc.participantId,
                    currency = expense.currency,
                    amountMinor = -alloc.allocatedMinor,
                    createdAt = expense.createdAt
                )
            )
        }
        return expense
    }

    @Synchronized
    override fun update(groupId: UUID, expenseId: UUID, update: ExpenseRecord, actorSubject: String?): ExpenseRecord {
        val existing = expenses[expenseId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Expense $expenseId not found")
        if (existing.groupId != groupId || existing.deleted) {
            throw ApplicationException(ErrorCode.ERR_05, "Expense $expenseId not found in group $groupId")
        }
        if (existing.version != update.version) {
            throw ApplicationException(ErrorCode.ERR_06, "Stale version: expected ${existing.version}, but got ${update.version}")
        }
        val groupPostings = postings.computeIfAbsent(groupId) { mutableListOf() }
        val now = Instant.now()
        // Reversal postings
        existing.payers.forEach { payer ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = payer.participantId,
                    currency = existing.currency,
                    amountMinor = -payer.amountMinor,
                    createdAt = now
                )
            )
        }
        existing.allocations.forEach { alloc ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = alloc.participantId,
                    currency = existing.currency,
                    amountMinor = alloc.allocatedMinor,
                    createdAt = now
                )
            )
        }
        // New postings
        update.payers.forEach { payer ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = payer.participantId,
                    currency = update.currency,
                    amountMinor = payer.amountMinor,
                    createdAt = now
                )
            )
        }
        update.allocations.forEach { alloc ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = alloc.participantId,
                    currency = update.currency,
                    amountMinor = -alloc.allocatedMinor,
                    createdAt = now
                )
            )
        }
        val updated = update.copy(
            version = existing.version + 1,
            updatedAt = now
        )
        expenses[expenseId] = updated
        return updated
    }

    @Synchronized
    override fun delete(groupId: UUID, expenseId: UUID, version: Long?, actorSubject: String?) {
        val existing = expenses[expenseId]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Expense $expenseId not found")
        if (existing.groupId != groupId || existing.deleted) {
            throw ApplicationException(ErrorCode.ERR_05, "Expense $expenseId not found in group $groupId")
        }
        if (version != null && existing.version != version) {
            throw ApplicationException(ErrorCode.ERR_06, "Stale version: expected ${existing.version}, but got $version")
        }
        val groupPostings = postings.computeIfAbsent(groupId) { mutableListOf() }
        val now = Instant.now()
        // Reversal postings
        existing.payers.forEach { payer ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = payer.participantId,
                    currency = existing.currency,
                    amountMinor = -payer.amountMinor,
                    createdAt = now
                )
            )
        }
        existing.allocations.forEach { alloc ->
            groupPostings.add(
                BalancePostingEntity(
                    postingId = UuidGenerator.next(),
                    groupId = groupId,
                    expenseId = expenseId,
                    participantId = alloc.participantId,
                    currency = existing.currency,
                    amountMinor = alloc.allocatedMinor,
                    createdAt = now
                )
            )
        }
        val deleted = existing.copy(
            deleted = true,
            version = existing.version + 1,
            updatedAt = now
        )
        expenses[expenseId] = deleted
    }

    override fun findById(expenseId: UUID): ExpenseRecord? {
        val record = expenses[expenseId]
        return if (record != null && !record.deleted) record else null
    }

    override fun list(groupId: UUID, category: String?, cursor: String?, limit: Int): List<ExpenseRecord> {
        return expenses.values
            .filter { it.groupId == groupId && !it.deleted }
            .filter { category == null || it.category == category }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override fun balances(groupId: UUID): List<GroupBalanceItem> {
        val groupPostings = postings[groupId].orEmpty()
        return groupPostings
            .groupBy { it.participantId to it.currency }
            .map { (key, list) ->
                val (participantId, currency) = key
                val sum = list.sumOf { it.amountMinor }
                GroupBalanceItem(
                    participantId = participantId.toString(),
                    amount = MoneyDto(currency, sum.toString())
                )
            }
            .sortedBy { it.participantId }
    }
}
