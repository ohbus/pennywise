package com.subhrodip.squarewise.expensecore.expenses.domain

import java.util.UUID

/** Internal payer posting input. */
data class ExpensePayer(val participantId: UUID, val amountMinor: Long)
