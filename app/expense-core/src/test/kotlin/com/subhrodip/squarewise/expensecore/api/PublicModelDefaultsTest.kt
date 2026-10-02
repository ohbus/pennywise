package com.subhrodip.squarewise.expensecore.api

import com.subhrodip.squarewise.expensecore.groups.api.GroupMemberResponse
import com.subhrodip.squarewise.expensecore.groups.api.InviteResponse
import com.subhrodip.squarewise.expensecore.recurring.domain.RecurringExpenseOccurrence
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies default values and mutable persistence state exposed by public model contracts. */
class PublicModelDefaultsTest {
    @Test
    fun `group member and invite responses preserve optional defaults`() {
        val membershipId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val member = GroupMemberResponse(membershipId, groupId)
        val invite = InviteResponse("invite-token", Instant.parse("2026-01-01T00:00:00Z"))

        assertEquals(null, member.subject)
        assertEquals(null, member.displayName)
        assertEquals(false, member.isPlaceholder)
        assertEquals("ACTIVE", member.status)
        assertEquals(null, invite.placeholderId)
    }

    @Test
    fun `recurring occurrence defaults remain mutable for ORM lifecycle state`() {
        val occurrence = RecurringExpenseOccurrence(
            occurrenceId = UUID.randomUUID(),
            scheduleId = UUID.randomUUID(),
            occurrenceDate = LocalDate.of(2026, 1, 1)
        )
        val expenseId = UUID.randomUUID()

        assertEquals(null, occurrence.expenseId)
        occurrence.expenseId = expenseId
        assertEquals(expenseId, occurrence.expenseId)
        assertEquals(true, occurrence.createdAt <= Instant.now())
    }
}
