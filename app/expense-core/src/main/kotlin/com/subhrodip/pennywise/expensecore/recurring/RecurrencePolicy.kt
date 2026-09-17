package com.subhrodip.pennywise.expensecore.recurring

import java.time.LocalDate
import java.time.YearMonth

enum class RecurrenceFrequency { WEEKLY, MONTHLY }
data class RecurrenceSchedule(
    val scheduleId: String,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int? = null,
) {
    init {
        require(scheduleId.isNotBlank()) { "scheduleId must not be blank" }
        require(dayOfMonth == null || dayOfMonth in 1..31) {
            "dayOfMonth must be between 1 and 31"
        }
        require(frequency == RecurrenceFrequency.MONTHLY || dayOfMonth == null) {
            "dayOfMonth is only supported for monthly schedules"
        }
    }
}

class RecurrencePolicy {
    fun nextAfter(date: LocalDate, schedule: RecurrenceSchedule): LocalDate =
        Companion.nextAfter(date, schedule)

    fun nextAfter(date: LocalDate, schedule: RecurringExpenseSchedule): LocalDate =
        Companion.nextAfter(date, schedule)

    companion object {
        fun nextAfter(date: LocalDate, schedule: RecurrenceSchedule): LocalDate = when (schedule.frequency) {
            RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> {
                val month = YearMonth.from(date).plusMonths(1)
                month.atDay((schedule.dayOfMonth ?: date.dayOfMonth).coerceAtMost(month.lengthOfMonth()))
            }
        }

        fun nextAfter(date: LocalDate, schedule: RecurringExpenseSchedule): LocalDate =
            nextAfter(
                date,
                RecurrenceSchedule(
                    scheduleId = schedule.scheduleId.toString(),
                    frequency = schedule.frequency,
                    dayOfMonth = schedule.dayOfMonth
                )
            )
    }
}
