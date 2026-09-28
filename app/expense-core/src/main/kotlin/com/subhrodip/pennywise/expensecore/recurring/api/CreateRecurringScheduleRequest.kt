package com.subhrodip.pennywise.expensecore.recurring.api
import com.subhrodip.pennywise.expensecore.expenses.domain.ExpenseAllocation
import com.subhrodip.pennywise.expensecore.expenses.domain.ExpensePayer
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurrenceFrequency
import java.time.LocalDate
import java.util.UUID

/** Domain command for creating a recurring expense schedule. */
data class CreateRecurringScheduleRequest(
    val scheduleId: UUID? = null,
    val description: String,
    val amountMinor: Long,
    val currency: String,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val payers: List<ExpensePayer>? = null,
    val allocations: List<ExpenseAllocation>? = null
)
