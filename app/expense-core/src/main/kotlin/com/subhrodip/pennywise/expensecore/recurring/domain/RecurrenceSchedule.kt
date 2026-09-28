package com.subhrodip.pennywise.expensecore.recurring.domain

/** Validated recurrence settings used to calculate the next scheduled date. */
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
