package com.subhrodip.pennywise.db.config

/** Bounded JDBC pool settings for one database target. */
data class PoolProperties(
    var url: String = "",
    var username: String = "",
    var password: String = "",
    var maximumPoolSize: Int = 10,
    var connectionTimeoutMs: Long = 2_000,
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
