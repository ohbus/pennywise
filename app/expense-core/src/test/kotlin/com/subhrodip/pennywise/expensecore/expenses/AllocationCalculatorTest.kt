package com.subhrodip.pennywise.expensecore.expenses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AllocationCalculatorTest {
    @Test
    fun `equal split assigns remainders by participant id`() {
        val result = AllocationCalculator.equal(100, listOf("c", "a", "b"))
        assertEquals(mapOf("a" to 34L, "b" to 33L, "c" to 33L), result)
        assertEquals(100L, result.values.sum())
    }

    @Test
    fun `percentage split preserves exact total`() {
        val result = AllocationCalculator.percentage(101, mapOf("a" to 3333, "b" to 3333, "c" to 3334))
        assertEquals(101L, result.values.sum())
        assertEquals(mapOf("a" to 34L, "b" to 33L, "c" to 34L), result)
    }

    @Test
    fun `invalid percentages are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AllocationCalculator.percentage(100, mapOf("a" to 5000, "b" to 4000))
        }
    }

    @Test
    fun `empty and duplicate participants are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { AllocationCalculator.equal(1, emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { AllocationCalculator.equal(1, listOf("a", "a")) }
    }
}
