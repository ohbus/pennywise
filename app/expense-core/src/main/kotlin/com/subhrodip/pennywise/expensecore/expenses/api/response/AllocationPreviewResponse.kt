package com.subhrodip.pennywise.expensecore.expenses.api.response

/** Result of an equal-allocation preview, expressed in minor currency units. */
data class AllocationPreviewResponse(val totalMinor: String, val allocations: Map<String, String>)
