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

@SpringBootTest
@Transactional
class RecurringExpenseControllerTest @Autowired constructor(
    private val controller: RecurringExpenseController,
    private val groupStore: JpaGroupStore
) {

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
        val created = controller.createSchedule(group.groupId, createRequest, null)
        assertNotNull(created.scheduleId)
        assertEquals(group.groupId, created.groupId)
        assertEquals("Monthly Rent", created.description)
        assertEquals("USD", created.amount.currency)
        assertEquals("250000", created.amount.minor)
        assertEquals(RecurrenceFrequency.MONTHLY, created.frequency)
        assertEquals(1, created.dayOfMonth)
        assertFalse(created.paused)

        // 2. Get schedule
        val retrieved = controller.getSchedule(group.groupId, created.scheduleId, null)
        assertEquals(created.scheduleId, retrieved.scheduleId)
        assertEquals("Monthly Rent", retrieved.description)

        // 3. List schedules
        val list = controller.listSchedules(group.groupId, null)
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
        val updated = controller.updateSchedule(group.groupId, created.scheduleId, updateRequest, null)
        assertEquals("Monthly Rent & Water", updated.description)
        assertEquals("270000", updated.amount.minor)

        // 5. Pause schedule
        val paused = controller.pauseSchedule(group.groupId, created.scheduleId, null)
        assertTrue(paused.paused)

        // 6. Resume schedule
        val resumed = controller.resumeSchedule(group.groupId, created.scheduleId, null)
        assertFalse(resumed.paused)
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

        val createErr = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            controller.createSchedule(randomGroup, createRequest, null)
        }
        assertEquals(HttpStatus.NOT_FOUND, createErr.statusCode)

        val getErr = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            controller.getSchedule(randomGroup, randomSchedule, null)
        }
        assertEquals(HttpStatus.NOT_FOUND, getErr.statusCode)
    }
}
