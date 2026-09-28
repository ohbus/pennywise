package com.subhrodip.squarewise.bff.messaging.model


import com.subhrodip.squarewise.bff.realtime.GroupInvalidation

/** Successful event-processing result with fanout details. */
data class ProcessedConsumptionResult(
    val invalidation: GroupInvalidation,
    val deliveredQueues: Int
) : ConsumptionResult
