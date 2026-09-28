package com.subhrodip.pennywise.bff.messaging.model


import com.subhrodip.pennywise.bff.realtime.GroupInvalidation

/** Successful event-processing result with fanout details. */
data class ProcessedConsumptionResult(
    val invalidation: GroupInvalidation,
    val deliveredQueues: Int
) : ConsumptionResult
