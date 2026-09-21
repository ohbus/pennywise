package com.subhrodip.pennywise.db.config

import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** Provides the writer/reader datasource topology when explicitly enabled. */
@AutoConfiguration
@EnableConfigurationProperties(DbProperties::class)
@ConditionalOnProperty(prefix = "pennywise.db", name = ["enabled"], havingValue = "true")
class DbAutoConfiguration {
    /** Builds the routed datasource used by application JPA repositories. */
    @Bean
    @Primary
    fun pennywiseDataSource(properties: DbProperties): DataSource {
        properties.writer.validate("writer")
        properties.readers.forEach { (name, pool) -> pool.validate("readers.$name") }
        val writer = buildDataSource(properties.writer, "writer")
        val readers = properties.readers.mapValues { (name, pool) -> buildDataSource(pool, "reader.$name") }
        return DbRoutingDataSource(writer, readers)
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
