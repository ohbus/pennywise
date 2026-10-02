package com.subhrodip.squarewise.expensecore.persistence

import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.BalancePostingEntity
import com.subhrodip.squarewise.expensecore.expenses.persistence.entity.ExpenseIdempotencyEntity
import com.subhrodip.squarewise.expensecore.groups.domain.GroupInvitationEntity
import com.subhrodip.squarewise.expensecore.recurring.domain.RecurringExpenseOccurrence
import com.subhrodip.squarewise.expensecore.settlements.domain.SettlementStatus
import com.subhrodip.squarewise.expensecore.settlements.persistence.SettlementEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
