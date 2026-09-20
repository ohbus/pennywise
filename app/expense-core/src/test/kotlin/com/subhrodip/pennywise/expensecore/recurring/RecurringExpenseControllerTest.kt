package com.subhrodip.pennywise.expensecore.recurring

import com.subhrodip.pennywise.expensecore.groups.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.JpaGroupStore
import com.subhrodip.pennywise.ids.ApiEndpoints
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID
import java.security.Principal

@SpringBootTest
@Transactional
class RecurringExpenseControllerTest @Autowired constructor(
    private val controller: RecurringExpenseController,
    private val groupStore: JpaGroupStore
) {
    private val alice = Principal { "alice" }

    @Test
    fun `creates, inspects, lists, updates, pauses, and resumes recurring schedule`() {
        val group = groupStore.create("alice", CreateGroupRequest("Penthouse", "HOUSEHOLD", "USD"))

        val createRequest = CreateRecurringScheduleRequestDto(
            description = "Monthly Rent",
            amount = com.subhrodip.pennywise.expensecore.expenses.MoneyDto("USD", "250000"),
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            startDate = LocalDate.of(2026, 10, 1)
        )

        // 1. Create schedule
        val created = controller.createSchedule(group.groupId, createRequest, alice)
        assertNotNull(created.scheduleId)
        assertEquals(group.groupId, created.groupId)
        assertEquals("Monthly Rent", created.description)
        assertEquals("USD", created.amount.currency)
        assertEquals("250000", created.amount.minor)
        assertEquals(RecurrenceFrequency.MONTHLY, created.frequency)
        assertEquals(1, created.dayOfMonth)
        assertFalse(created.paused)

        // 2. Get schedule
        val retrieved = controller.getSchedule(group.groupId, created.scheduleId, alice)
        assertEquals(created.scheduleId, retrieved.scheduleId)
        assertEquals("Monthly Rent", retrieved.description)

        // 3. List schedules
        val list = controller.listSchedules(group.groupId, alice)
        assertEquals(1, list.size)
        assertEquals(created.scheduleId, list[0].scheduleId)

        // 4. Update schedule
        val updateRequest = UpdateRecurringScheduleRequestDto(
            description = "Monthly Rent & Water",
            amount = com.subhrodip.pennywise.expensecore.expenses.MoneyDto("USD", "270000"),
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            startDate = LocalDate.of(2026, 10, 1)
        )
        val updated = controller.updateSchedule(group.groupId, created.scheduleId, updateRequest, alice)
        assertEquals("Monthly Rent & Water", updated.description)
        assertEquals("270000", updated.amount.minor)

        // 5. Pause schedule
        val paused = controller.pauseSchedule(group.groupId, created.scheduleId, alice)
        assertTrue(paused.paused)
        val pausedReplay = controller.pauseSchedule(group.groupId, created.scheduleId, alice)
        assertTrue(pausedReplay.paused)

        // 6. Resume schedule
        val resumed = controller.resumeSchedule(group.groupId, created.scheduleId, alice)
        assertFalse(resumed.paused)
        val resumedReplay = controller.resumeSchedule(group.groupId, created.scheduleId, alice)
        assertFalse(resumedReplay.paused)
    }

    @Test
    fun `throws 404 for non-existent group or schedule`() {
        val randomGroup = UUID.randomUUID()
        val randomSchedule = UUID.randomUUID()

        val createRequest = CreateRecurringScheduleRequestDto(
            description = "Internet",
            amount = com.subhrodip.pennywise.expensecore.expenses.MoneyDto("EUR", "5000"),
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.now()
        )

        val createErr = org.junit.jupiter.api.assertThrows<com.subhrodip.pennywise.errors.ApplicationException> {
            controller.createSchedule(randomGroup, createRequest, alice)
        }
        assertEquals(com.subhrodip.pennywise.errors.ErrorCode.ERR_05, createErr.errorCode)

        val getErr = org.junit.jupiter.api.assertThrows<com.subhrodip.pennywise.errors.ApplicationException> {
            controller.getSchedule(randomGroup, randomSchedule, alice)
        }
        assertEquals(com.subhrodip.pennywise.errors.ErrorCode.ERR_05, getErr.errorCode)
    }

    @Test
    fun `rejects missing principal for an existing group`() {
        val group = groupStore.create("alice", CreateGroupRequest("No Anonymous Schedules", "TRIP", "EUR"))
        val request = CreateRecurringScheduleRequestDto(
            description = "Unauthorized",
            amount = com.subhrodip.pennywise.expensecore.expenses.MoneyDto("EUR", "100"),
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2026, 10, 1)
        )

        val error = org.junit.jupiter.api.assertThrows<com.subhrodip.pennywise.errors.ApplicationException> {
            controller.createSchedule(group.groupId, request, null)
        }
        assertEquals(com.subhrodip.pennywise.errors.ErrorCode.ERR_03, error.errorCode)
    }
}
