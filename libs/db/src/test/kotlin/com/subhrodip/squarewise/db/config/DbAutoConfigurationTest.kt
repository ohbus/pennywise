package com.subhrodip.squarewise.db.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Verifies fail-fast validation for the scheduled reader-health configuration. */
class DbAutoConfigurationTest {
    private val configuration = DbAutoConfiguration()

    @Test
    fun `returns a valid health probe interval`() {
        assertEquals(
            2_000L,
            configuration.squarewiseDbHealthProbeIntervalMs(
                DbProperties(readerLagBudgetMs = 5_000, healthProbeIntervalMs = 2_000)
            )
        )
    }

    @Test
    fun `rejects non-positive replay lag budget`() {
        assertFailsWith<IllegalArgumentException> {
            configuration.squarewiseDbHealthProbeIntervalMs(
                DbProperties(readerLagBudgetMs = 0, healthProbeIntervalMs = 2_000)
            )
        }
    }

    @Test
    fun `rejects health probe intervals outside the scheduler bounds`() {
        listOf(249L, 120_001L).forEach { interval ->
            assertFailsWith<IllegalArgumentException> {
                configuration.squarewiseDbHealthProbeIntervalMs(
                    DbProperties(readerLagBudgetMs = 5_000, healthProbeIntervalMs = interval)
                )
            }
        }
    }
}
