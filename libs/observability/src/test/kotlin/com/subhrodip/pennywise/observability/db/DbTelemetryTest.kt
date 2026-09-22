package com.subhrodip.pennywise.observability.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class DbTelemetryTest {
    @Test
    fun `retains fallback and failure diagnostics without a metrics registry`() {
        val telemetry = DbTelemetry(slowQueryThresholdMs = 100)
        telemetry.failure("search")
        telemetry.acquisition("expense.search", "reader", 3)
        telemetry.lockWait("expense.search")
        telemetry.deadlock("expense.search")
        telemetry.queryDuration("expense.search", "reader", 120)
        assertEquals(DbTelemetrySnapshot(failures = 1, acquisitions = 1, acquisitionTotalMs = 3, lockWaits = 1, deadlocks = 1, queries = 1, queryTotalMs = 120, slowQueries = 1), telemetry.snapshot())
    }
}
