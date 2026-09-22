package com.subhrodip.pennywise.expensecore.expenses.api.response

import com.subhrodip.pennywise.expensecore.expenses.api.request.MoneyDto
/** One participant's balance in a group. */
data class GroupBalanceItem(val participantId: String, val amount: MoneyDto)
