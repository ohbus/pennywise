package com.subhrodip.pennywise.expensecore.recurring
import com.subhrodip.pennywise.expensecore.expenses.domain.ExpenseAllocation
import com.subhrodip.pennywise.expensecore.expenses.domain.ExpensePayer
import com.subhrodip.pennywise.expensecore.expenses.persistence.store.ExpenseStore
import com.subhrodip.pennywise.expensecore.groups.api.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.api.CreateInviteRequest
import com.subhrodip.pennywise.expensecore.groups.persistence.store.JpaGroupStore
import com.subhrodip.pennywise.expensecore.messaging.outbox.persistence.OutboxStore
import com.subhrodip.pennywise.expensecore.recurring.api.CreateRecurringScheduleRequest
import com.subhrodip.pennywise.expensecore.recurring.api.UpdateRecurringScheduleRequest
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurrenceFrequency
import com.subhrodip.pennywise.expensecore.recurring.persistence.RecurringExpenseOccurrenceRepository
import com.subhrodip.pennywise.expensecore.recurring.persistence.RecurringExpenseScheduleRepository
import com.subhrodip.pennywise.expensecore.recurring.service.RecurringExpenseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Transactional
class RecurringExpenseServiceTest @Autowired constructor(
    private val service: RecurringExpenseService,
    private val groupStore: JpaGroupStore,
    private val expenseStore: ExpenseStore,
    private val scheduleRepository: RecurringExpenseScheduleRepository,
    private val occurrenceRepository: RecurringExpenseOccurrenceRepository,
    private val outboxStore: OutboxStore
) {

    @Test
    fun `creates schedule successfully with weekly frequency and initial next occurrence date`() {
        val group = groupStore.create("alice", CreateGroupRequest("Apartment 4B", "HOUSEHOLD", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Weekly Cleaning",
                amountMinor = 5000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        assertNotNull(schedule.scheduleId)
        assertEquals(group.groupId, schedule.groupId)
        assertEquals("Weekly Cleaning", schedule.description)
        assertEquals(5000, schedule.amountMinor)
        assertEquals("EUR", schedule.currency)
        assertEquals(RecurrenceFrequency.WEEKLY, schedule.frequency)
        assertEquals(startDate, schedule.startDate)
        assertEquals(startDate, schedule.nextOccurrenceDate)
        assertFalse(schedule.paused)

        val retrieved = service.getSchedule(schedule.scheduleId)
        assertNotNull(retrieved)
        assertEquals(schedule.scheduleId, retrieved?.scheduleId)

        val list = service.listSchedules(group.groupId)
        assertEquals(1, list.size)
        assertEquals(schedule.scheduleId, list[0].scheduleId)
    }

    @Test
    fun `updates schedule properties successfully`() {
        val group = groupStore.create("alice", CreateGroupRequest("Apartment 4B", "HOUSEHOLD", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Weekly Cleaning",
                amountMinor = 5000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        val updated = service.updateSchedule(
            group.groupId,
            schedule.scheduleId,
            UpdateRecurringScheduleRequest(
                description = "Biweekly Deep Clean",
                amountMinor = 8000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        assertEquals("Biweekly Deep Clean", updated.description)
        assertEquals(8000, updated.amountMinor)
    }

    @Test
    fun `pauses and resumes schedule`() {
        val group = groupStore.create("alice", CreateGroupRequest("Cabin", "TRIP", "EUR"))
        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Weekly Supplies",
                amountMinor = 2000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = LocalDate.of(2026, 9, 1)
            )
        )

        val paused = service.pauseSchedule(schedule.scheduleId)
        assertTrue(paused.paused)

        val retrievedPaused = service.getSchedule(schedule.scheduleId)
        assertTrue(retrievedPaused?.paused == true)

        val resumed = service.resumeSchedule(schedule.scheduleId)
        assertFalse(resumed.paused)

        val retrievedResumed = service.getSchedule(schedule.scheduleId)
        assertFalse(retrievedResumed?.paused == true)
    }

    @Test
    fun `pause and resume throw 404 for non-existent schedule`() {
        val missingId = UUID.randomUUID()
        val pauseErr = assertThrows(ApplicationException::class.java) {
            service.pauseSchedule(missingId)
        }
        assertEquals(ErrorCode.ERR_05, pauseErr.errorCode)

        val resumeErr = assertThrows(ApplicationException::class.java) {
            service.resumeSchedule(missingId)
        }
        assertEquals(ErrorCode.ERR_05, resumeErr.errorCode)
    }

    @Test
    fun `processes due weekly occurrences, advances next date, and creates expense split equally`() {
        val group = groupStore.create("alice", CreateGroupRequest("Shared Flat", "HOUSEHOLD", "EUR"))
        val invite = groupStore.invite(group.groupId, "alice", CreateInviteRequest(24))
        groupStore.claim(invite.token, "bob")

        val startDate = LocalDate.of(2026, 9, 1)
        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Internet bill",
                amountMinor = 4000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        val processed = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(1, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(1, occurrences.size)
        val occurrence = occurrences[0]
        assertEquals(startDate, occurrence.occurrenceDate)
        assertNotNull(occurrence.expenseId)

        val expense = expenseStore.findById(occurrence.expenseId!!)
        assertNotNull(expense)
        assertEquals("Internet bill", expense?.description)
        assertEquals(4000, expense?.amountMinor)
        assertEquals("EUR", expense?.currency)
        assertEquals(2, expense?.allocations?.size)
        assertEquals(listOf(2000L, 2000L), expense?.allocations?.map { it.allocatedMinor }?.sorted())

        val updatedSchedule = service.getSchedule(schedule.scheduleId)
        assertEquals(LocalDate.of(2026, 9, 8), updatedSchedule?.nextOccurrenceDate)

        val processedAgain = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(0, processedAgain)
    }

    @Test
    fun `processes multiple missed occurrences up to asOfDate`() {
        val group = groupStore.create("alice", CreateGroupRequest("Road Trip", "TRIP", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Vehicle maintenance",
                amountMinor = 3000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        val processed = service.processDueOccurrences(asOfDate = LocalDate.of(2026, 9, 15))
        assertEquals(3, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(3, occurrences.size)
        val dates = occurrences.map { it.occurrenceDate }.sorted()
        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 15)
            ),
            dates
        )

        val updatedSchedule = service.getSchedule(schedule.scheduleId)
        assertEquals(LocalDate.of(2026, 9, 22), updatedSchedule?.nextOccurrenceDate)
    }

    @Test
    fun `bounds worker catch-up occurrences to max limit`() {
        val group = groupStore.create("alice", CreateGroupRequest("Long Trip", "TRIP", "EUR"))
        val startDate = LocalDate.of(2026, 1, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Weekly Fuel",
                amountMinor = 3000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        // As of 20 weeks later, but maxCatchUpOccurrences set to 5
        val processed = service.processDueOccurrences(asOfDate = LocalDate.of(2026, 5, 20), maxCatchUpOccurrences = 5)
        assertEquals(5, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(5, occurrences.size)
    }

    @Test
    fun `pauses schedule and emits outbox notification on invalid membership`() {
        val group = groupStore.create("alice", CreateGroupRequest("Private Flat", "HOUSEHOLD", "EUR"))
        val aliceId = UUID.nameUUIDFromBytes("alice".toByteArray(StandardCharsets.UTF_8))
        val nonMemberId = UUID.randomUUID()
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Custom Split Invalid Member",
                amountMinor = 6000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate,
                payers = listOf(ExpensePayer(aliceId, 6000)),
                allocations = listOf(ExpenseAllocation(aliceId, 3000), ExpenseAllocation(nonMemberId, 3000))
            )
        )

        val processed = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(0, processed)

        val updated = service.getSchedule(schedule.scheduleId)
        assertTrue(updated?.paused == true)

        val outboxMessages = outboxStore.snapshot()
        val notificationMsg = outboxMessages.find { it.eventType == "recurring.schedule.paused" }
        assertNotNull(notificationMsg)
        assertEquals(schedule.scheduleId, notificationMsg?.aggregateId)
        assertEquals("invalid_membership", notificationMsg?.payload?.get("reason"))
    }

    @Test
    fun `does not process paused schedules`() {
        val group = groupStore.create("alice", CreateGroupRequest("Ski House", "HOUSEHOLD", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Firewood",
                amountMinor = 1500,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        service.pauseSchedule(schedule.scheduleId)

        val processed = service.processDueOccurrences(asOfDate = LocalDate.of(2026, 9, 15))
        assertEquals(0, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(0, occurrences.size)

        val updatedSchedule = service.getSchedule(schedule.scheduleId)
        assertEquals(startDate, updatedSchedule?.nextOccurrenceDate)
    }

    @Test
    fun `respects end date and stops generating occurrences`() {
        val group = groupStore.create("alice", CreateGroupRequest("Summer Rent", "HOUSEHOLD", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)
        val endDate = LocalDate.of(2026, 9, 8)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Weekly rent",
                amountMinor = 10000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate,
                endDate = endDate
            )
        )

        val processed = service.processDueOccurrences(asOfDate = LocalDate.of(2026, 9, 20))
        assertEquals(2, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(2, occurrences.size)
        assertEquals(listOf(startDate, endDate), occurrences.map { it.occurrenceDate }.sorted())

        val processedAgain = service.processDueOccurrences(asOfDate = LocalDate.of(2026, 9, 25))
        assertEquals(0, processedAgain)
    }

    @Test
    fun `handles monthly schedule recurrence and month end clamping`() {
        val group = groupStore.create("alice", CreateGroupRequest("Home", "HOUSEHOLD", "USD"))
        val startDate = LocalDate.of(2026, 1, 31)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Monthly gym",
                amountMinor = 5000,
                currency = "USD",
                frequency = RecurrenceFrequency.MONTHLY,
                dayOfMonth = 31,
                startDate = startDate
            )
        )

        val processed = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(1, processed)

        val updated = service.getSchedule(schedule.scheduleId)
        assertEquals(LocalDate.of(2026, 2, 28), updated?.nextOccurrenceDate)
    }

    @Test
    fun `supports specified custom payers and allocations`() {
        val group = groupStore.create("alice", CreateGroupRequest("Studio", "HOUSEHOLD", "EUR"))
        val invite = groupStore.invite(group.groupId, "alice", CreateInviteRequest(24))
        groupStore.claim(invite.token, "bob")

        val aliceId = UUID.nameUUIDFromBytes("alice".toByteArray(StandardCharsets.UTF_8))
        val bobId = UUID.nameUUIDFromBytes("bob".toByteArray(StandardCharsets.UTF_8))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Custom Split Power",
                amountMinor = 6000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate,
                payers = listOf(ExpensePayer(aliceId, 6000)),
                allocations = listOf(ExpenseAllocation(aliceId, 4000), ExpenseAllocation(bobId, 2000))
            )
        )

        val processed = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(1, processed)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        val expense = expenseStore.findById(occurrences[0].expenseId!!)
        assertNotNull(expense)
        assertEquals(listOf(ExpensePayer(aliceId, 6000)), expense?.payers)
        assertEquals(
            listOf(ExpenseAllocation(aliceId, 4000), ExpenseAllocation(bobId, 2000)),
            expense?.allocations
        )
    }

    @Test
    fun `idempotency skips already generated occurrence`() {
        val group = groupStore.create("alice", CreateGroupRequest("Dorm", "HOUSEHOLD", "EUR"))
        val startDate = LocalDate.of(2026, 9, 1)

        val schedule = service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Wifi",
                amountMinor = 2000,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = startDate
            )
        )

        val processed = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(1, processed)

        schedule.nextOccurrenceDate = startDate
        scheduleRepository.save(schedule)

        val processedDuplicate = service.processDueOccurrences(asOfDate = startDate)
        assertEquals(0, processedDuplicate)

        val occurrences = service.getOccurrences(schedule.scheduleId)
        assertEquals(1, occurrences.size)
    }

    @Test
    fun `validation rejects invalid inputs`() {
        val group = groupStore.create("alice", CreateGroupRequest("Test", "HOUSEHOLD", "EUR"))

        assertThrows(IllegalArgumentException::class.java) {
            service.createSchedule(
                group.groupId,
                CreateRecurringScheduleRequest(
                    description = " ",
                    amountMinor = 1000,
                    currency = "EUR",
                    frequency = RecurrenceFrequency.WEEKLY,
                    startDate = LocalDate.now()
                )
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            service.createSchedule(
                group.groupId,
                CreateRecurringScheduleRequest(
                    description = "Sub",
                    amountMinor = 0,
                    currency = "EUR",
                    frequency = RecurrenceFrequency.WEEKLY,
                    startDate = LocalDate.now()
                )
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            service.createSchedule(
                group.groupId,
                CreateRecurringScheduleRequest(
                    description = "Sub",
                    amountMinor = 1000,
                    currency = "INVALID",
                    frequency = RecurrenceFrequency.WEEKLY,
                    startDate = LocalDate.now()
                )
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            service.createSchedule(
                group.groupId,
                CreateRecurringScheduleRequest(
                    description = "Sub",
                    amountMinor = 1000,
                    currency = "EUR",
                    frequency = RecurrenceFrequency.WEEKLY,
                    dayOfMonth = 15,
                    startDate = LocalDate.now()
                )
            )
        }

        assertThrows(ApplicationException::class.java) {
            service.createSchedule(
                UUID.randomUUID(),
                CreateRecurringScheduleRequest(
                    description = "Sub",
                    amountMinor = 1000,
                    currency = "EUR",
                    frequency = RecurrenceFrequency.WEEKLY,
                    startDate = LocalDate.now()
                )
            )
        }
    }
}
