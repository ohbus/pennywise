package com.subhrodip.pennywise.expensecore.recurring.service
import com.subhrodip.pennywise.expensecore.expenses.api.request.MoneyDto
import com.subhrodip.pennywise.expensecore.recurring.api.RecurringScheduleResponse
import com.subhrodip.pennywise.expensecore.recurring.domain.RecurringExpenseSchedule
/** Maps recurring schedule domain entities to API responses. */
fun RecurringExpenseSchedule.toResponse(): RecurringScheduleResponse = RecurringScheduleResponse(
    scheduleId = scheduleId,
    groupId = groupId,
    description = description,
    amount = MoneyDto(currency, amountMinor.toString()),
    frequency = frequency,
    dayOfMonth = dayOfMonth,
    startDate = startDate,
    endDate = endDate,
    nextOccurrenceDate = nextOccurrenceDate,
    paused = paused,
    createdAt = createdAt,
    version = version
)
