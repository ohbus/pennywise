package com.subhrodip.squarewise.observability.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DbTelemetryTest {

    @Test
    fun `measureQuery records successful and failed blocks`() {
        val telemetry = DbTelemetry(slowQueryThresholdMs = 10)

        telemetry.route("groups", "writer")
        telemetry.failure("reader")
        telemetry.acquisition("groups", "writer", -5)
        telemetry.lockWait("groups")
        telemetry.deadlock("expenses")
        telemetry.lag("reader", 12)

        assertEquals("value", telemetry.measureQuery("groups", "writer") { "value" })
        assertThrows(IllegalStateException::class.java) {
            telemetry.measureQuery("expenses", "reader") {
                throw IllegalStateException("query failed")
            }
        }

        val snapshot = telemetry.snapshot()
        assertEquals(1, snapshot.failures)
        assertEquals(1, snapshot.acquisitions)
        assertEquals(0, snapshot.acquisitionTotalMs)
        assertEquals(1, snapshot.lockWaits)
        assertEquals(1, snapshot.deadlocks)
        assertEquals(2, snapshot.queries)
        assertEquals(0, snapshot.slowQueries)
        assertTrue(snapshot.queryTotalMs >= 0)

        telemetry.queryDuration("slow", "reader", 10)
        assertEquals(1, telemetry.snapshot().slowQueries)
    }

    @Test
    fun `rejects a non-positive slow query threshold`() {
        assertThrows(IllegalArgumentException::class.java) {
            DbTelemetry(slowQueryThresholdMs = 0)
        }
    }
}
