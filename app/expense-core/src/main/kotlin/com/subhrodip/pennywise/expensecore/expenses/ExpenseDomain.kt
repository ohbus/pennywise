package com.subhrodip.pennywise.expensecore.expenses

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/** Shared bounds for public Expense Core mutation inputs. */
object ExpenseRequestLimits {
    const val MAX_PARTICIPANTS = 100
    const val MAX_CATEGORY_LENGTH = 32
    const val MAX_IDEMPOTENCY_KEY_LENGTH = 200
}

data class MoneyDto(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String,

    @field:NotBlank
    @field:Pattern(regexp = "^-?[0-9]+$")
    val minor: String
)

data class PayerDto(
    @field:NotBlank
    val participantId: String,

    @field:Valid
    val amount: MoneyDto
)

data class AllocationItemDto(
    @field:NotBlank
    val participantId: String,

    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]+$")
    val value: String
)

data class AllocationInputDto(
    @field:NotBlank
    @field:Pattern(regexp = "^(EQUAL|EXACT|PERCENT_BASIS_POINTS|WEIGHTED_SHARES)$")
    val mode: String,

    @field:NotEmpty
    val items: List<@Valid AllocationItemDto>
)

data class CreateExpenseRequest(
    val expenseId: UUID,

    @field:NotBlank
    @field:Size(min = 1, max = 240)
    val description: String,

    @field:Size(max = ExpenseRequestLimits.MAX_CATEGORY_LENGTH)
    val category: String? = "other",

    @field:Valid
    val amount: MoneyDto,

    @field:NotEmpty
    @field:Size(max = ExpenseRequestLimits.MAX_PARTICIPANTS)
    val payers: List<@Valid PayerDto>,

    @field:Valid
    val allocation: AllocationInputDto
)

data class UpdateExpenseRequest(
    @field:Min(1)
    val version: Long,

    @field:NotBlank
    @field:Size(min = 1, max = 240)
    val description: String,

    @field:Size(max = ExpenseRequestLimits.MAX_CATEGORY_LENGTH)
    val category: String? = "other",

    @field:Valid
    val amount: MoneyDto,

    @field:NotEmpty
    @field:Size(max = ExpenseRequestLimits.MAX_PARTICIPANTS)
    val payers: List<@Valid PayerDto>,

    @field:Valid
    val allocation: AllocationInputDto
)

data class ExpenseAllocationResponse(
    val participantId: String,
    val amount: MoneyDto
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseResponse(
    val expenseId: UUID,
    val version: Long,
    val amount: MoneyDto,
    val category: String,
    val allocations: List<ExpenseAllocationResponse>
)

data class GroupBalanceItem(
    val participantId: String,
    val amount: MoneyDto
)

data class GroupBalancesResponse(
    val groupId: UUID,
    val balances: List<GroupBalanceItem>
)

data class ExpensePayer(
    val participantId: UUID,
    val amountMinor: Long
)

data class ExpenseAllocation(
    val participantId: UUID,
    val allocatedMinor: Long
)

data class ExpenseRecord(
    val expenseId: UUID,
    val groupId: UUID,
    val description: String,
    val category: String,
    val currency: String,
    val amountMinor: Long,
    val version: Long,
    val allocationMode: String,
    val createdAt: Instant,
    val payers: List<ExpensePayer>,
    val allocations: List<ExpenseAllocation>,
    val deleted: Boolean = false,
    val updatedAt: Instant? = null
)
