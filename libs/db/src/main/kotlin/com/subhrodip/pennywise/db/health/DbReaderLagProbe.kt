package com.subhrodip.pennywise.db.health

import javax.sql.DataSource
import org.springframework.scheduling.annotation.Scheduled
import com.subhrodip.pennywise.observability.db.DbTelemetry

/** Measures PostgreSQL physical-replication replay lag without touching application tables. */
class DbReaderLagProbe {
    /**
     * Returns replay lag in milliseconds, or null when PostgreSQL has not replayed
     * a transaction timestamp yet.
     */
    fun measure(dataSource: DataSource): Long? = dataSource.connection.use { connection ->
        connection.prepareStatement(SQL).use { statement ->
            statement.executeQuery().use { result ->
                if (!result.next()) null else result.getLong(1).takeUnless { result.wasNull() }
            }
        }
    }

    private companion object {
        const val SQL = """
            SELECT CASE
                WHEN pg_last_xact_replay_timestamp() IS NULL THEN NULL
                ELSE EXTRACT(EPOCH FROM (clock_timestamp() - pg_last_xact_replay_timestamp())) * 1000
            END
        """
    }
}

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
                val lagMs = probe.measure(dataSource)
                lagMs?.let { telemetry.lag(name, it) }
                if (lagMs == null || lagMs > lagBudgetMs) health.markLagging(name) else health.markHealthy(name)
            } catch (_: Exception) {
                health.markDisconnected(name)
            }
        }
    }
}
