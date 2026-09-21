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
        assertEquals(DbTelemetrySnapshot(fallbacks = 1, failures = 1, acquisitions = 1, acquisitionTotalMs = 3), telemetry.snapshot())
    }
}
