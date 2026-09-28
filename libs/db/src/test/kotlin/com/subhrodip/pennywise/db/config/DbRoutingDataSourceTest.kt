package com.subhrodip.pennywise.db.config

import com.subhrodip.pennywise.db.routing.DbContextHolder
import com.subhrodip.pennywise.db.routing.DbExecutionContext
import com.subhrodip.pennywise.db.routing.DbOperationKind
import com.subhrodip.pennywise.db.routing.DbRoute
import com.subhrodip.pennywise.db.routing.ReadConsistency
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** Verifies that explicit datasource routes cannot bypass operation safety policy. */
class DbRoutingDataSourceTest {
    @Test
    fun `writer-only context rejects explicit reader connection`() {
        val writer = mock(DataSource::class.java)
        val reader = mock(DataSource::class.java)
        val routing = DbRoutingDataSource(writer, mapOf("replica" to reader))

        assertFailsWith<IllegalStateException> {
            DbContextHolder.withContext(DbExecutionContext("expense.create", DbOperationKind.COMMAND)) {
                routing.connection(DbRoute.READER, "replica")
            }
        }
    }

    @Test
    fun `approved eventual query may acquire explicit reader connection`() {
        val writer = mock(DataSource::class.java)
        val reader = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        `when`(reader.connection).thenReturn(connection)
        val routing = DbRoutingDataSource(writer, mapOf("replica" to reader))

        DbContextHolder.withContext(
            DbExecutionContext("expense.search", DbOperationKind.QUERY, ReadConsistency.EVENTUAL, readerEligible = true)
        ) {
            kotlin.test.assertSame(connection, routing.connection(DbRoute.READER, "replica"))
        }
    }
}
