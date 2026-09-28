package com.subhrodip.squarewise.expensecore.expenses.domain

import java.util.UUID

/** Internal participant allocation input. */
data class ExpenseAllocation(val participantId: UUID, val allocatedMinor: Long)
