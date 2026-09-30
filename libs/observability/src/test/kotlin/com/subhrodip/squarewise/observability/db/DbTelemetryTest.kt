package com.subhrodip.squarewise.observability.db

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.core.instrument.MeterRegistry
import org.mockito.Mockito.mock

class DbTelemetryTest {
    @Test
    fun `retains fallback and failure diagnostics without a metrics registry`() {
        val telemetry = DbTelemetry(slowQueryThresholdMs = 100)
        telemetry.failure("search")
        telemetry.acquisition("expense.search", "reader", 3)
        telemetry.lockWait("expense.search")
        telemetry.deadlock("expense.search")
        telemetry.queryDuration("expense.search", "reader", 120)
        telemetry.lag("reader", 50)
        assertEquals(DbTelemetrySnapshot(failures = 1, acquisitions = 1, acquisitionTotalMs = 3, lockWaits = 1, deadlocks = 1, queries = 1, queryTotalMs = 120, slowQueries = 1), telemetry.snapshot())
    }

    /** Registry-backed calls emit bounded metric names while preserving in-process counters. */
    @Test
    fun `records registry metrics and normalizes negative durations`() {
        val registry = SimpleMeterRegistry()
        val telemetry = DbTelemetry(registry, slowQueryThresholdMs = 100)

        telemetry.route("expense.search", "reader")
        telemetry.failure("replica")
        telemetry.acquisition("expense.search", "reader", -5)
        telemetry.queryDuration("expense.search", "reader", -1)
        telemetry.queryDuration("expense.search", "reader", 100)
        telemetry.lockWait("expense.search")
        telemetry.deadlock("expense.search")
        telemetry.lag("replica", 42)

        val snapshot = telemetry.snapshot()
        assertEquals(1, snapshot.failures)
        assertEquals(1, snapshot.acquisitions)
        assertEquals(0, snapshot.acquisitionTotalMs)
        assertEquals(2, snapshot.queries)
        assertEquals(100, snapshot.queryTotalMs)
        assertEquals(1, snapshot.slowQueries)
        assertEquals(1.0, registry.get("squarewise.db.route").counter().count())
        assertEquals(1.0, registry.get("squarewise.db.reader.failure").counter().count())
    }

    /** Timed blocks always record duration and propagate failures unchanged. */
    @Test
    fun `measure query records both successful and failed blocks`() {
        val telemetry = DbTelemetry(slowQueryThresholdMs = 1)
        assertEquals("ok", telemetry.measureQuery("expense.search", "reader") { "ok" })
        assertThrows(IllegalStateException::class.java) {
            telemetry.measureQuery("expense.search", "reader") { throw IllegalStateException("query failed") }
        }
        assertEquals(2, telemetry.snapshot().queries)
    }

    /** A non-positive slow-query threshold is rejected at construction. */
    @Test
    fun `rejects non-positive slow query threshold`() {
        assertThrows(IllegalArgumentException::class.java) { DbTelemetry(slowQueryThresholdMs = 0) }
    }

    /** A registry that cannot supply meters is treated as a safe no-op adapter. */
    @Test
    fun `tolerates missing registry meters`() {
        val registry = mock(MeterRegistry::class.java)
        val telemetry = DbTelemetry(registry)

        telemetry.route("expense.search", "reader")
        telemetry.acquisition("expense.search", "reader", 1)
        telemetry.queryDuration("expense.search", "reader", 1)
        telemetry.lag("replica", 1)

        assertEquals(1, telemetry.snapshot().acquisitions)
        assertEquals(1, telemetry.snapshot().queries)
    }
}
