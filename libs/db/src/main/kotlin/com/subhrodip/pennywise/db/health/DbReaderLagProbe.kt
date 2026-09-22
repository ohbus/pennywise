package com.subhrodip.pennywise.db.health

import javax.sql.DataSource
import com.subhrodip.pennywise.db.routing.DbWatermark

/** Measures PostgreSQL physical-replication replay lag without touching application tables. */
class DbReaderLagProbe {
    /**
     * Returns replay lag in milliseconds, or null when PostgreSQL has not replayed
     * a transaction timestamp yet.
     */
    fun measure(dataSource: DataSource): Long? = measureResult(dataSource).lagMs

    /** Returns replay lag and the last replayed LSN in one probe. */
    fun measureResult(dataSource: DataSource): DbReaderProbeResult = dataSource.connection.use { connection ->
        connection.prepareStatement(SQL).use { statement ->
            statement.executeQuery().use { result ->
                if (!result.next()) DbReaderProbeResult(null, null)
                else DbReaderProbeResult(
                    result.getLong(1).takeUnless { result.wasNull() },
                    result.getString(2)?.let(DbWatermark::parse)
                )
            }
        }
    }

    private companion object {
        const val SQL = """
            SELECT CASE
                WHEN pg_last_xact_replay_timestamp() IS NULL THEN NULL
                ELSE EXTRACT(EPOCH FROM (clock_timestamp() - pg_last_xact_replay_timestamp())) * 1000
            END, pg_last_wal_replay_lsn()::text
        """
    }
}
