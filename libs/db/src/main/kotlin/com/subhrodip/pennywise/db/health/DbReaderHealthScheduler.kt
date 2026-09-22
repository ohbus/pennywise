package com.subhrodip.pennywise.db.health

import com.subhrodip.pennywise.observability.db.DbTelemetry
import javax.sql.DataSource
import org.springframework.scheduling.annotation.Scheduled

/** Applies bounded replay-lag probes to the shared reader circuit state. */
class DbReaderHealthScheduler(
    private val readers: Map<String, DataSource>,
    private val health: DbReaderHealth,
    private val lagBudgetMs: Long,
    private val probe: DbReaderLagProbe = DbReaderLagProbe(),
    private val telemetry: DbTelemetry = DbTelemetry()
) {
    init { require(lagBudgetMs > 0) { "lagBudgetMs must be positive" } }

    /** Probes each named reader once; a failed probe never touches the writer. */
    @Scheduled(fixedDelayString = "#{@pennywiseDbHealthProbeIntervalMs}")
    fun probeReaders() {
        readers.forEach { (name, dataSource) ->
            try {
                val result = probe.measureResult(dataSource)
                result.lagMs?.let { telemetry.lag(name, it) }
                if (result.lagMs == null || result.lagMs > lagBudgetMs) health.markLagging(name)
                else health.markHealthy(name, result.replayedWatermark)
            } catch (_: Exception) {
                health.markDisconnected(name)
            }
        }
    }
}
