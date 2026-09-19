/**
 * Utility functions for validating and converting expense request payloads.
 * Centralises duplicated validation logic from `ExpenseController` to adhere to DRY principles.
 */
package com.subhrodip.pennywise.expensecore.expenses

import org.springframework.http.HttpStatus
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.util.UUID

object ExpenseValidator {
    /** Parse and validate the amount minor unit */
    fun parseAndValidateAmount(minorStr: String, fieldName: String = "amount.minor"): Long {
        val totalMinor = minorStr.toLongOrNull()
            ?: throw ApplicationException(ErrorCode.ERR_02, "$fieldName must be a valid integer")
        if (totalMinor <= 0) {
            throw ApplicationException(ErrorCode.ERR_02, "$fieldName must be greater than zero")
        }
        return totalMinor
    }

    /** Validate payer amounts and currency match */
    fun validatePayers(payers: List<PayerDto>, expenseCurrency: String, fieldPrefix: String = "payer"): Long {
        var sum = 0L
        payers.forEach { payer ->
            val pAmount = payer.amount.minor.toLongOrNull()
                ?: throw ApplicationException(ErrorCode.ERR_02, "$fieldPrefix amount.minor must be a valid integer")
            if (pAmount <= 0) {
                throw ApplicationException(ErrorCode.ERR_02, "$fieldPrefix amount.minor must be positive")
            }
            if (payer.amount.currency != expenseCurrency) {
                throw ApplicationException(ErrorCode.ERR_02, "$fieldPrefix currency must match expense currency")
            }
            sum += pAmount
        }
        return sum
    }

    /** Map payer DTOs to domain objects */
    fun mapDomainPayers(payers: List<PayerDto>): List<ExpensePayer> =
        payers.map {
            ExpensePayer(
                participantId = UUID.fromString(it.participantId),
                amountMinor = it.amount.minor.toLong()
            )
        }

    /** Map allocation map to domain objects */
    fun mapDomainAllocations(allocationMap: Map<String, Long>): List<ExpenseAllocation> =
        allocationMap.map { (participantIdStr, allocatedMinor) ->
            ExpenseAllocation(
                participantId = UUID.fromString(participantIdStr),
                allocatedMinor = allocatedMinor
            )
        }
}
