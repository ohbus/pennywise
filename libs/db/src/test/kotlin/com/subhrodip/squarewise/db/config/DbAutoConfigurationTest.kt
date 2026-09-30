package com.subhrodip.squarewise.db.config

import com.subhrodip.squarewise.db.health.DbReaderHealth
import com.subhrodip.squarewise.observability.db.DbTelemetry
import com.zaxxer.hikari.HikariDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
}
