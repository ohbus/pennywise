package com.subhrodip.pennywise.expensecore.recurring

import com.subhrodip.pennywise.expensecore.expenses.MoneyDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Data transfer object representing a payer allocation in a recurring schedule request.
 */
data class ExpensePayerDto(
    @field:NotBlank
    val participantId: String,

    @field:Valid
    val amount: MoneyDto
)

/**
 * Data transfer object representing a split allocation item in a recurring schedule request.
 */
data class ExpenseAllocationItemDto(
    @field:NotBlank
    val participantId: String,

    @field:Valid
    val amount: MoneyDto
)

/**
 * Transport payload for creating a new recurring expense schedule.
 */
data class CreateRecurringScheduleRequestDto(
    @field:NotBlank
    @field:Size(min = 1, max = 240)
    val description: String,

    @field:Valid
    val amount: MoneyDto,

    @field:NotNull
    val frequency: RecurrenceFrequency,

    val dayOfMonth: Int? = null,

    @field:NotNull
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    val payers: List<@Valid ExpensePayerDto>? = null,

    val allocations: List<@Valid ExpenseAllocationItemDto>? = null
)

/**
 * Transport payload for updating an existing recurring expense schedule.
 */
data class UpdateRecurringScheduleRequestDto(
    @field:NotBlank
    @field:Size(min = 1, max = 240)
    val description: String,

    @field:Valid
    val amount: MoneyDto,

    @field:NotNull
    val frequency: RecurrenceFrequency,

    val dayOfMonth: Int? = null,

    @field:NotNull
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    val payers: List<@Valid ExpensePayerDto>? = null,

    val allocations: List<@Valid ExpenseAllocationItemDto>? = null
)

/**
 * API response representing a recurring expense schedule configuration.
 */
data class RecurringScheduleResponse(
    val scheduleId: UUID,
    val groupId: UUID,
    val description: String,
    val amount: MoneyDto,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val nextOccurrenceDate: LocalDate,
    val paused: Boolean,
    val createdAt: Instant,
    val version: Long
)

/**
 * Converts a domain entity [RecurringExpenseSchedule] into a transport response DTO [RecurringScheduleResponse].
 */
fun RecurringExpenseSchedule.toResponse(): RecurringScheduleResponse =
    RecurringScheduleResponse(
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
