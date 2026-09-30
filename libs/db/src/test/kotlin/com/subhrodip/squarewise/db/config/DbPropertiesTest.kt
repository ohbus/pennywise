package com.subhrodip.squarewise.db.config

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies connection-pool configuration rejects unsafe incomplete settings. */
class DbPropertiesTest {
    private val validPool = PoolProperties(
        url = "jdbc:postgresql://writer/db",
        username = "app",
        maximumPoolSize = 10,
        connectionTimeoutMs = 2_000,
        maxLifetimeMs = 1_800_000,
    )
    @Test
    fun `diagnostic reader alias is explicit and disabled by default`() {
        assertFalse(DbProperties().readerIsWriterDiagnostic)
        assertTrue(DbProperties(readerIsWriterDiagnostic = true).readerIsWriterDiagnostic)
    }

    @Test
    fun `writer endpoint is required`() {
        assertFailsWith<IllegalArgumentException> { PoolProperties().validate("writer") }
    }

    @Test
    fun `pool size is bounded`() {
        assertFailsWith<IllegalArgumentException> {
            PoolProperties(url = "jdbc:postgresql://writer/db", username = "app", maximumPoolSize = 201)
                .validate("writer")
        }
        assertFailsWith<IllegalArgumentException> { validPool.copy(maximumPoolSize = 0).validate("writer") }
    }

    /** Every required pool bound rejects values outside its documented safe range. */
    @Test
    fun `pool endpoint and timing bounds are enforced`() {
        assertFailsWith<IllegalArgumentException> { validPool.copy(username = "").validate("writer") }
        assertFailsWith<IllegalArgumentException> { validPool.copy(url = " ").validate("writer") }
        assertFailsWith<IllegalArgumentException> { validPool.copy(username = " ").validate("writer") }
        assertFailsWith<IllegalArgumentException> {
            validPool.copy(connectionTimeoutMs = 249).validate("writer")
        }
        assertFailsWith<IllegalArgumentException> {
            validPool.copy(connectionTimeoutMs = 120_001).validate("writer")
        }
        assertFailsWith<IllegalArgumentException> {
            validPool.copy(maxLifetimeMs = 29_999).validate("writer")
        }
        validPool.copy(maxLifetimeMs = 30_000).validate("writer")
        assertFailsWith<IllegalArgumentException> {
            validPool.copy(maxLifetimeMs = Long.MIN_VALUE).validate("writer")
        }
    }
}
