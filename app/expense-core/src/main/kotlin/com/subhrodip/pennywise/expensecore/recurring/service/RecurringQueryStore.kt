package com.subhrodip.pennywise.expensecore.recurring.service
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurringExpenseOccurrence
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurringExpenseSchedule
import java.util.UUID

/** Reader-side recurring schedule and occurrence query port. */
interface RecurringQueryStore {
    fun getSchedule(scheduleId: UUID): RecurringExpenseSchedule?
    fun listSchedules(groupId: UUID): List<RecurringExpenseSchedule>
    fun getOccurrences(scheduleId: UUID): List<RecurringExpenseOccurrence>
}
