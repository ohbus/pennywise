package com.subhrodip.pennywise.db.config

import com.zaxxer.hikari.HikariDataSource
import com.subhrodip.pennywise.db.health.DbReaderHealth
import com.subhrodip.pennywise.db.health.DbReaderHealthScheduler
import com.subhrodip.pennywise.observability.db.DbTelemetry
import javax.sql.DataSource
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.beans.factory.ObjectProvider
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.EnableScheduling

/** Provides the writer/reader datasource topology when explicitly enabled. */
@AutoConfiguration
@EnableConfigurationProperties(DbProperties::class)
@EnableScheduling
@ConditionalOnProperty(prefix = "pennywise.db", name = ["enabled"], havingValue = "true")
class DbAutoConfiguration {
    /** Creates telemetry even when an application has no metrics registry, preserving diagnostics. */
    @Bean
    fun pennywiseDbTelemetry(registry: ObjectProvider<MeterRegistry>): DbTelemetry = DbTelemetry(registry.getIfAvailable())
    /** Exposes the bounded scheduler interval to the scheduled probe expression. */
    @Bean(name = ["pennywiseDbHealthProbeIntervalMs"])
    fun pennywiseDbHealthProbeIntervalMs(properties: DbProperties): Long {
        require(properties.readerLagBudgetMs > 0) { "pennywise.db.reader-lag-budget-ms must be positive" }
        require(properties.healthProbeIntervalMs in 250..120_000) { "pennywise.db.health-probe-interval-ms is invalid" }
        return properties.healthProbeIntervalMs
    }
    /** Supplies the shared reader circuit state used by the routed datasource. */
    @Bean
    fun pennywiseReaderHealth(): DbReaderHealth = DbReaderHealth()

    /** Builds the routed datasource used by application JPA repositories. */
    @Bean
    @Primary
    fun pennywiseDataSource(properties: DbProperties, readerHealth: DbReaderHealth, telemetry: DbTelemetry): DataSource {
        properties.writer.validate("writer")
        properties.readers.forEach { (name, pool) -> pool.validate("readers.$name") }
        val writer = buildDataSource(properties.writer, "writer")
        val readers = properties.readers.mapValues { (name, pool) -> buildDataSource(pool, "reader.$name") }
        return DbRoutingDataSource(writer, readers, readerHealth, telemetry)
    }

    /** Creates a reader-only replay-lag scheduler; it never probes the writer. */
    @Bean
    fun pennywiseReaderHealthScheduler(
        properties: DbProperties,
        readerHealth: DbReaderHealth,
        dataSource: DataSource,
        telemetry: DbTelemetry
    ): DbReaderHealthScheduler {
        val readers = (dataSource as? DbRoutingDataSource)?.readerDataSources().orEmpty()
        return DbReaderHealthScheduler(readers, readerHealth, properties.readerLagBudgetMs, telemetry = telemetry)
    }

    private fun buildDataSource(properties: PoolProperties, poolName: String): HikariDataSource =
        DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .url(properties.url)
            .username(properties.username)
            .password(properties.password)
            .build()
            .also {
                it.poolName = "pennywise-$poolName"
                it.maximumPoolSize = properties.maximumPoolSize
                it.connectionTimeout = properties.connectionTimeoutMs
                it.maxLifetime = properties.maxLifetimeMs
            }
}
