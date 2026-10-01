package com.subhrodip.squarewise.db.config

import com.subhrodip.squarewise.db.health.DbReaderHealth
import com.subhrodip.squarewise.observability.db.DbTelemetry
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.mockito.Mockito.mock

/** Verifies fail-fast validation for the scheduled reader-health configuration. */
class DbAutoConfigurationTest {
    private val configuration = DbAutoConfiguration()

    @Test
    fun `returns a valid health probe interval`() {
        assertEquals(
            2_000L,
            configuration.squarewiseDbHealthProbeIntervalMs(
                DbProperties(readerLagBudgetMs = 5_000, healthProbeIntervalMs = 2_000)
            )
        )
    }

    @Test
    fun `rejects non-positive replay lag budget`() {
        assertFailsWith<IllegalArgumentException> {
            configuration.squarewiseDbHealthProbeIntervalMs(
                DbProperties(readerLagBudgetMs = 0, healthProbeIntervalMs = 2_000)
            )
        }
    }

    @Test
    fun `rejects health probe intervals outside the scheduler bounds`() {
        listOf(249L, 120_001L).forEach { interval ->
            assertFailsWith<IllegalArgumentException> {
                configuration.squarewiseDbHealthProbeIntervalMs(
                    DbProperties(readerLagBudgetMs = 5_000, healthProbeIntervalMs = interval)
                )
            }
        }
    }

    @Test
    fun `diagnostic mode aliases every configured reader to the writer pool`() {
        val writer = mock(HikariDataSource::class.java)
        val pool = PoolProperties(
            url = "jdbc:postgresql://writer/db",
            username = "app",
        )
        val properties = DbProperties(
            writer = pool,
            readers = mapOf("replica" to pool),
            readerIsWriterDiagnostic = true,
        )

        val routing = configuration.squarewiseDataSource(
            properties,
            writer,
            DbReaderHealth(),
            DbTelemetry(),
        ) as DbRoutingDataSource

        assertSame(writer, routing.readerDataSources()["replica"])
    }

    /** Verifies normal mode builds an independent reader pool rather than aliasing the writer. */
    @Test
    fun `normal mode builds configured reader datasource`() {
        val writer = mock(HikariDataSource::class.java)
        val writerPool = PoolProperties(
            url = "jdbc:postgresql://writer/db",
            username = "app",
        )
        val readerPool = PoolProperties(
            url = "jdbc:postgresql://reader/db",
            username = "app",
        )
        val routing = configuration.squarewiseDataSource(
            DbProperties(writer = writerPool, readers = mapOf("replica" to readerPool)),
            writer,
            DbReaderHealth(),
            DbTelemetry(),
        ) as DbRoutingDataSource

        val reader = routing.readerDataSources()["replica"] as HikariDataSource
        try {
            assertNotSame(writer, reader)
            assertEquals("jdbc:postgresql://reader/db", reader.jdbcUrl)
            assertEquals("app", reader.username)
        } finally {
            reader.close()
        }
    }

    @Test
    fun `reader health scheduler accepts routed and ordinary datasources`() {
        val properties = DbProperties(readerLagBudgetMs = 250)
        val health = DbReaderHealth()
        val telemetry = DbTelemetry()
        val ordinary = mock(DataSource::class.java)
        val routed = DbRoutingDataSource(ordinary, emptyMap())

        val ordinaryScheduler = configuration.squarewiseReaderHealthScheduler(
            properties,
            health,
            ordinary,
            telemetry,
        )
        val routedScheduler = configuration.squarewiseReaderHealthScheduler(
            properties,
            health,
            routed,
            telemetry,
        )

        assertEquals(Unit, ordinaryScheduler.probeReaders())
        assertEquals(Unit, routedScheduler.probeReaders())
    }
}
