package com.subhrodip.pennywise.expensecore.expenses.api.response

import java.util.UUID

/** Group balance response. */
data class GroupBalancesResponse(val groupId: UUID, val balances: List<GroupBalanceItem>)
