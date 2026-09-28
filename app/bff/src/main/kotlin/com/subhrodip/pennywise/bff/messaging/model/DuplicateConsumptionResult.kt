package com.subhrodip.pennywise.bff.messaging.model

import java.util.UUID

/** Result returned when an event has already been consumed. */
data class DuplicateConsumptionResult(val eventId: UUID) : ConsumptionResult
