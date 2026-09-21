package com.subhrodip.pennywise.db.config

import com.subhrodip.pennywise.db.routing.DbRoute
import com.subhrodip.pennywise.db.routing.DbContextHolder
import com.subhrodip.pennywise.db.health.DbReaderDecision
import com.subhrodip.pennywise.db.health.DbReaderHealth
import com.subhrodip.pennywise.observability.db.DbTelemetry
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import org.springframework.jdbc.datasource.AbstractDataSource

/** Routes connections to the writer or a named read pool at acquisition time. */
class DbRoutingDataSource(
    private val writer: DataSource,
    private val readers: Map<String, DataSource>,
    private val readerHealth: DbReaderHealth = DbReaderHealth(),
    private val telemetry: DbTelemetry = DbTelemetry()
) : AbstractDataSource() {
    init { readers.keys.forEach(readerHealth::register) }

    override fun getConnection(): Connection = connection(routeForCurrentContext())

    override fun getConnection(username: String?, password: String?): Connection = connection(routeForCurrentContext())

    /** Returns a connection for an explicit route. */
    fun connection(route: DbRoute, readerName: String = readers.keys.firstOrNull() ?: ""): Connection {
        if (route == DbRoute.WRITER) {
            telemetry.route(DbContextHolder.current().operationName, "writer")
            return writer.connection
        }
        val reader = readers[readerName] ?: error("No configured reader pool named '$readerName'")
        return try {
            reader.connection
                .also { telemetry.route(DbContextHolder.current().operationName, "reader") }
        } catch (failure: SQLException) {
            readerHealth.markFailure(readerName)
            telemetry.failure(readerName)
            if (readerHealth.route(DbContextHolder.current(), readerName) == DbReaderDecision.BoundedWriterFallback) {
                telemetry.fallback(DbContextHolder.current().operationName)
                writer.connection
            } else {
                throw failure
            }
        }
    }

    /** Returns the configured reader pools for health probing and diagnostics. */
    fun readerDataSources(): Map<String, DataSource> = readers.toMap()

    private fun routeForCurrentContext(): DbRoute = when (readerHealth.route(DbContextHolder.current(), readers.keys.firstOrNull() ?: "")) {
        DbReaderDecision.Reader -> DbRoute.READER
        DbReaderDecision.Writer, DbReaderDecision.BoundedWriterFallback -> DbRoute.WRITER
        DbReaderDecision.Fail -> throw SQLException("No healthy reader is available for query '${DbContextHolder.current().operationName}'")
    }
}
