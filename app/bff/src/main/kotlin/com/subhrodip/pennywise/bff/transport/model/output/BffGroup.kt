package com.subhrodip.pennywise.bff.transport.model.output

/** Aggregate group representation exposed by the BFF. */
data class BffGroup(val groupId: String, val name: String, val kind: String? = null, val status: String? = null, val revision: Long = 0, val balances: List<BffBalance> = emptyList(), val expenses: List<BffExpense> = emptyList(), val members: List<BffMember> = emptyList()) { val id: String get() = groupId }
