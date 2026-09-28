package com.subhrodip.squarewise.expensecore.expenses.api.response

import com.subhrodip.squarewise.expensecore.expenses.api.request.MoneyDto
/** One participant's balance in a group. */
data class GroupBalanceItem(val participantId: String, val amount: MoneyDto)
