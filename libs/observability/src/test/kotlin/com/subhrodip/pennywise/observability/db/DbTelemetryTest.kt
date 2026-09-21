package com.subhrodip.pennywise.observability.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class DbTelemetryTest {
    @Test
    fun `retains fallback and failure diagnostics without a metrics registry`() {
        val telemetry = DbTelemetry()
        telemetry.fallback("expense.search")
        telemetry.failure("search")
        telemetry.acquisition("expense.search", "reader", 3)
        telemetry.lockWait("expense.search")
        telemetry.deadlock("expense.search")
        assertEquals(DbTelemetrySnapshot(fallbacks = 1, failures = 1, acquisitions = 1, acquisitionTotalMs = 3, lockWaits = 1, deadlocks = 1), telemetry.snapshot())
    }
}
