package com.subhrodip.pennywise.expensecore.recurring.domain

import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

class OccurrenceIdentity {
    fun id(scheduleId: String, occurrenceDate: LocalDate): UUID =
        Companion.id(scheduleId, occurrenceDate)

    fun id(scheduleId: UUID, occurrenceDate: LocalDate): UUID =
        Companion.id(scheduleId.toString(), occurrenceDate)

    companion object {
        fun id(scheduleId: String, occurrenceDate: LocalDate): UUID {
            require(scheduleId.isNotBlank()) { "scheduleId must not be blank" }
            return UUID.nameUUIDFromBytes(
                "$scheduleId:$occurrenceDate".toByteArray(StandardCharsets.UTF_8),
            )
        }

        fun id(scheduleId: UUID, occurrenceDate: LocalDate): UUID =
            id(scheduleId.toString(), occurrenceDate)
    }
}
