package com.subhrodip.pennywise.observability.db

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong
import java.time.Duration

/** Low-cardinality database route and reader-health telemetry. */
class DbTelemetry(private val registry: MeterRegistry? = null) {
    private val fallbackCount = AtomicLong()
    private val failureCount = AtomicLong()
    private val acquisitionCount = AtomicLong()
    private val acquisitionTotalMs = AtomicLong()
    private val lockWaitCount = AtomicLong()
    private val deadlockCount = AtomicLong()

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

    /** Records bounded pool-acquisition timing without SQL text or parameters. */
    fun acquisition(operation: String, pool: String, durationMs: Long) {
        acquisitionCount.incrementAndGet()
        acquisitionTotalMs.addAndGet(durationMs.coerceAtLeast(0))
        registry?.timer("pennywise.db.pool.acquisition", Tags.of("operation", operation, "pool", pool))
            ?.record(Duration.ofMillis(durationMs.coerceAtLeast(0)))
    }

    /** Records a lock-wait observation using only the stable operation label. */
    fun lockWait(operation: String) {
        lockWaitCount.incrementAndGet()
        registry?.counter("pennywise.db.lock.wait", "operation", operation)?.increment()
    }

    /** Records a deadlock observation using only the stable operation label. */
    fun deadlock(operation: String) {
        deadlockCount.incrementAndGet()
        registry?.counter("pennywise.db.deadlock", "operation", operation)?.increment()
    }

    /** Records the latest replay lag in milliseconds for a named reader. */
    fun lag(reader: String, lagMs: Long) {
        registry?.gauge("pennywise.db.reader.lag.ms", Tags.of("reader", reader), lagMs)
    }

    /** Snapshot counters useful to tests and diagnostic endpoints. */
    fun snapshot(): DbTelemetrySnapshot = DbTelemetrySnapshot(
        fallbacks = fallbackCount.get(),
        failures = failureCount.get(),
        acquisitions = acquisitionCount.get(),
        acquisitionTotalMs = acquisitionTotalMs.get(),
        lockWaits = lockWaitCount.get(),
        deadlocks = deadlockCount.get()
    )
}

/** In-process counters retained even when no metrics registry is configured. */
data class DbTelemetrySnapshot(
    val fallbacks: Long,
    val failures: Long,
    val acquisitions: Long = 0,
    val acquisitionTotalMs: Long = 0,
    val lockWaits: Long = 0,
    val deadlocks: Long = 0
)
