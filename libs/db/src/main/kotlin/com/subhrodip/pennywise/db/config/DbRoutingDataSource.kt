package com.subhrodip.pennywise.db.config

import com.subhrodip.pennywise.db.routing.DbRoute
import java.sql.Connection
import javax.sql.DataSource
import org.springframework.jdbc.datasource.AbstractDataSource

/** Routes connections to the writer or a named read pool at acquisition time. */
class DbRoutingDataSource(
    private val writer: DataSource,
    private val readers: Map<String, DataSource>
) : AbstractDataSource() {
    override fun getConnection(): Connection = writer.connection

    override fun getConnection(username: String?, password: String?): Connection = writer.getConnection(username, password)

    /** Returns a connection for an explicit route. */
    fun connection(route: DbRoute, readerName: String = readers.keys.firstOrNull() ?: ""): Connection =
        when (route) {
            DbRoute.WRITER -> writer.connection
            DbRoute.READER -> readers[readerName]?.connection
                ?: error("No configured reader pool named '$readerName'")
        }
}
