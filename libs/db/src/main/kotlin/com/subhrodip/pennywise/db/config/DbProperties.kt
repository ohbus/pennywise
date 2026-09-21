package com.subhrodip.pennywise.db.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the CQRS writer and named read pools. */
@ConfigurationProperties("pennywise.db")
data class DbProperties(
    /** Enables the shared routed datasource instead of framework defaults. */
    var enabled: Boolean = false,
    /** Authoritative writer JDBC settings. */
    var writer: PoolProperties = PoolProperties(),
    /** Named read-only pool settings. */
    var readers: Map<String, PoolProperties> = emptyMap(),
    /** Explicit local-only mode that aliases configured reader names to the writer pool. */
    var readerIsWriterDiagnostic: Boolean = false,
    /** Maximum tolerated asynchronous replay lag before a reader is marked lagging. */
    var readerLagBudgetMs: Long = 5_000,
    /** Bounded interval between replay-lag probes. */
    var healthProbeIntervalMs: Long = 2_000
)

/** Bounded JDBC pool settings for one database target. */
data class PoolProperties(
    /** JDBC URL. */
    var url: String = "",
    /** Database username. */
    var username: String = "",
    /** Database password. */
    var password: String = "",
    /** Maximum physical connections in this pool. */
    var maximumPoolSize: Int = 10,
    /** Connection acquisition timeout in milliseconds. */
    var connectionTimeoutMs: Long = 2_000,
    /** Maximum lifetime in milliseconds. */
    var maxLifetimeMs: Long = 1_800_000
) {
    /** Validates safety-critical pool bounds and required endpoint data. */
    fun validate(name: String) {
        require(url.isNotBlank()) { "pennywise.db.$name.url is required" }
        require(username.isNotBlank()) { "pennywise.db.$name.username is required" }
        require(maximumPoolSize in 1..200) { "pennywise.db.$name.maximum-pool-size must be between 1 and 200" }
        require(connectionTimeoutMs in 250..120_000) { "pennywise.db.$name.connection-timeout-ms is invalid" }
        require(maxLifetimeMs >= 30_000) { "pennywise.db.$name.max-lifetime-ms is invalid" }
    }
}
