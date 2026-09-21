package com.subhrodip.pennywise.observability.db

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

/** Low-cardinality database route and reader-health telemetry. */
class DbTelemetry(private val registry: MeterRegistry? = null) {
    private val fallbackCount = AtomicLong()
    private val failureCount = AtomicLong()

    /** Records a connection route for a validated operation name. */
    fun route(operation: String, route: String) {
        registry?.counter("pennywise.db.route", Tags.of("operation", operation, "route", route))?.increment()
    }

    /** Records a bounded writer fallback from a reader query. */
    fun fallback(operation: String) {
        fallbackCount.incrementAndGet()
        registry?.counter("pennywise.db.fallback", "operation", operation)?.increment()
    }

    /** Records a reader connection/probe failure. */
    fun failure(reader: String) {
        failureCount.incrementAndGet()
        registry?.counter("pennywise.db.reader.failure", "reader", reader)?.increment()
    }

    /** Records the latest replay lag in milliseconds for a named reader. */
    fun lag(reader: String, lagMs: Long) {
        registry?.gauge("pennywise.db.reader.lag.ms", Tags.of("reader", reader), lagMs)
    }

    /** Snapshot counters useful to tests and diagnostic endpoints. */
    fun snapshot(): DbTelemetrySnapshot = DbTelemetrySnapshot(fallbackCount.get(), failureCount.get())
}

/** In-process counters retained even when no metrics registry is configured. */
data class DbTelemetrySnapshot(val fallbacks: Long, val failures: Long)
