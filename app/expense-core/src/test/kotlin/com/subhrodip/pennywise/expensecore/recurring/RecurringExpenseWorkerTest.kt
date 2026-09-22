package com.subhrodip.pennywise.expensecore.recurring
import com.subhrodip.pennywise.expensecore.groups.api.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.persistence.store.JpaGroupStore
import com.subhrodip.pennywise.expensecore.recurring.api.CreateRecurringScheduleRequest
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurrenceFrequency
import com.subhrodip.pennywise.expensecore.recurring.service.RecurringExpenseService
import com.subhrodip.pennywise.expensecore.recurring.service.RecurringExpenseWorker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(properties = ["pennywise.recurring.enabled=true", "pennywise.recurring.poll-delay-ms=5000"])
@Transactional
class RecurringExpenseWorkerTest @Autowired constructor(
    private val worker: RecurringExpenseWorker,
    private val service: RecurringExpenseService,
    private val groupStore: JpaGroupStore
) {

    @Test
    fun `worker executes due occurrences when enabled`() {
        val group = groupStore.create("alice", CreateGroupRequest("Apartment 101", "HOUSEHOLD", "EUR"))
        val today = LocalDate.now()

        service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Daily Paper",
                amountMinor = 300,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = today
            )
        )

        // Enabled worker executes and returns count
        worker.enabled = true
        val processed = worker.run()
        assertEquals(1, processed)

        // Immediate subsequent execution has no more due occurrences
        val secondRun = worker.run()
        assertEquals(0, secondRun)
    }

    @Test
    fun `worker does not execute when disabled`() {
        val group = groupStore.create("alice", CreateGroupRequest("Apartment 102", "HOUSEHOLD", "EUR"))
        val today = LocalDate.now()

        service.createSchedule(
            group.groupId,
            CreateRecurringScheduleRequest(
                description = "Daily Milk",
                amountMinor = 200,
                currency = "EUR",
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = today
            )
        )

        // Disable worker
        worker.enabled = false
        val processed = worker.run()
        assertEquals(0, processed)

        // Re-enable and verify it runs
        worker.enabled = true
        val processedAfterEnable = worker.run()
        assertEquals(1, processedAfterEnable)
    }
}
