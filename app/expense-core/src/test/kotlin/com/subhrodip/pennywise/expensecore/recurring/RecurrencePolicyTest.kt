package com.subhrodip.pennywise.expensecore.recurring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecurrencePolicyTest {
    private val policy = RecurrencePolicy()
    @Test
    fun `clamps monthly schedule at month end`() {
        assertEquals(LocalDate.of(2024, 2, 29), policy.nextAfter(LocalDate.of(2024, 1, 31), RecurrenceSchedule("s", RecurrenceFrequency.MONTHLY, 31)))
    }
    @Test
    fun `advances weekly occurrence`() {
        assertEquals(LocalDate.of(2026, 9, 24), policy.nextAfter(LocalDate.of(2026, 9, 17), RecurrenceSchedule("s", RecurrenceFrequency.WEEKLY)))
    }

    @Test
    fun `rejects invalid schedule identity`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceSchedule(" ", RecurrenceFrequency.WEEKLY)
        }
    }

    @Test
    fun `rejects invalid monthly day`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceSchedule("s", RecurrenceFrequency.MONTHLY, 32)
        }
    }

    @Test
    fun `rejects monthly day on weekly schedule`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceSchedule("s", RecurrenceFrequency.WEEKLY, 1)
        }
    }
}
