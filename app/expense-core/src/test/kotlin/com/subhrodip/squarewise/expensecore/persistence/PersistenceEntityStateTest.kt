package com.subhrodip.squarewise.expensecore.persistence

import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.BalancePostingEntity
import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.ExpenseIdempotencyEntity
import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.ExpenseAllocationEntity
import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.ExpenseEntity
import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.ExpensePayerEntity
import com.subhrodip.squarewise.expensecore.groups.domain.GroupAuditEntity
import com.subhrodip.squarewise.expensecore.groups.domain.GroupInvitationEntity
import com.subhrodip.squarewise.expensecore.recurring.domain.RecurringExpenseOccurrence
import com.subhrodip.squarewise.expensecore.settlements.domain.SettlementStatus
import com.subhrodip.squarewise.expensecore.settlements.persistence.SettlementEntity
import com.subhrodip.squarewise.expensecore.sync.persistence.SyncChangeEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/** Verifies durable identity and lifecycle state for financial persistence records. */
class PersistenceEntityStateTest {
    @Test
    fun `settlement and invitation retain lifecycle transitions`() {
        val groupId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val to = UUID.randomUUID()
        val settlement = SettlementEntity(UUID.randomUUID(), groupId, from, to, 1250)
        settlement.status = SettlementStatus.REVERSED
        settlement.reversalReason = "Duplicate external payment"

        val placeholderId = UUID.randomUUID()
        val invitation = GroupInvitationEntity(
            token = "a".repeat(64),
            groupId = groupId,
            expiresAt = Instant.parse("2026-10-03T12:00:00Z"),
            placeholderId = placeholderId
        )
        val claimedAt = Instant.parse("2026-10-02T12:00:00Z")
        invitation.claimedAt = claimedAt
        invitation.claimedBy = "alice"

        assertEquals(groupId, settlement.groupId)
        assertEquals(from, settlement.fromParticipantId)
        assertEquals(to, settlement.toParticipantId)
        assertEquals(1250, settlement.amountMinor)
        assertEquals(SettlementStatus.REVERSED, settlement.status)
        assertEquals("Duplicate external payment", settlement.reversalReason)
        assertEquals(groupId, invitation.groupId)
        assertEquals(placeholderId, invitation.placeholderId)
        assertEquals(claimedAt, invitation.claimedAt)
        assertEquals("alice", invitation.claimedBy)
        assertEquals(null, invitation.revokedAt)
    }

    @Test
    fun `ledger posting and idempotency claim retain their owning references`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val settlementId = UUID.randomUUID()
        val participantId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-10-02T12:00:00Z")
        val posting = BalancePostingEntity(
            postingId = UUID.randomUUID(),
            groupId = groupId,
            expenseId = expenseId,
            settlementId = settlementId,
            participantId = participantId,
            currency = "EUR",
            amountMinor = -1250,
            createdAt = createdAt
        )
        val claim = ExpenseIdempotencyEntity(
            idempotencyId = UUID.randomUUID(),
            groupId = groupId,
            actorSubject = "alice",
            operation = "CREATE_EXPENSE",
            idempotencyKey = "request-1",
            payloadHash = "hash-1",
            expenseId = expenseId,
            createdAt = createdAt
        )

        assertEquals(groupId, posting.groupId)
        assertEquals(expenseId, posting.expenseId)
        assertEquals(settlementId, posting.settlementId)
        assertEquals(participantId, posting.participantId)
        assertEquals("EUR", posting.currency)
        assertEquals(-1250, posting.amountMinor)
        assertEquals(createdAt, posting.createdAt)
        assertEquals(groupId, claim.groupId)
        assertEquals("alice", claim.actorSubject)
        assertEquals("CREATE_EXPENSE", claim.operation)
        assertEquals("request-1", claim.idempotencyKey)
        assertEquals("hash-1", claim.payloadHash)
        assertEquals(expenseId, claim.expenseId)
        assertEquals(createdAt, claim.createdAt)
    }

    @Test
    fun `recurring occurrence links a schedule date before and after expense creation`() {
        val scheduleId = UUID.randomUUID()
        val occurrence = RecurringExpenseOccurrence(
            occurrenceId = UUID.randomUUID(),
            scheduleId = scheduleId,
            occurrenceDate = LocalDate.parse("2026-10-02")
        )
        val expenseId = UUID.randomUUID()
        occurrence.expenseId = expenseId

        assertEquals(scheduleId, occurrence.scheduleId)
        assertEquals(LocalDate.parse("2026-10-02"), occurrence.occurrenceDate)
        assertEquals(expenseId, occurrence.expenseId)
        assertEquals(false, occurrence.createdAt.isAfter(Instant.now()))
    }

    @Test
    fun `payer and allocation records retain expense participant amounts`() {
        val expense = mock(ExpenseEntity::class.java)
        val participantId = UUID.randomUUID()
        val payer = ExpensePayerEntity(UUID.randomUUID(), expense, participantId, 900)
        val allocation = ExpenseAllocationEntity(UUID.randomUUID(), expense, participantId, 900)
        payer.amountMinor = 1000
        allocation.allocatedMinor = 1000

        assertEquals(expense, payer.expense)
        assertEquals(participantId, payer.participantId)
        assertEquals(1000, payer.amountMinor)
        assertEquals(expense, allocation.expense)
        assertEquals(participantId, allocation.participantId)
        assertEquals(1000, allocation.allocatedMinor)
    }

    @Test
    fun `sync change and audit records expose revision and event state`() {
        val createdAt = Instant.parse("2026-10-02T12:00:00Z")
        val change = SyncChangeEntity(
            changeId = UUID.randomUUID(),
            groupId = "group-1",
            revision = 4,
            entityId = "expense-1",
            deleted = true,
            payload = null,
            createdAt = createdAt
        )
        val audit = GroupAuditEntity(
            auditId = UUID.randomUUID(),
            groupId = UUID.randomUUID(),
            subject = "alice",
            action = "group.renamed",
            revision = 4,
            payload = "{\"name\":\"Trip\"}",
            occurredAt = createdAt
        )
        change.payload = "{\"kind\":\"updated\"}"
        change.deleted = false
        audit.subject = "bob"
        audit.action = "group.updated"
        audit.payload = "{\"name\":\"Holiday\"}"

        assertEquals("group-1", change.groupId)
        assertEquals(4, change.revision)
        assertEquals("expense-1", change.entityId)
        assertEquals(false, change.deleted)
        assertEquals("{\"kind\":\"updated\"}", change.payload)
        assertEquals(createdAt, change.createdAt)
        assertEquals("bob", audit.subject)
        assertEquals("group.updated", audit.action)
        assertEquals(4, audit.revision)
        assertEquals("{\"name\":\"Holiday\"}", audit.payload)
        assertEquals(createdAt, audit.occurredAt)
    }
}
