package com.subhrodip.pennywise.db.config

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Verifies connection-pool configuration rejects unsafe incomplete settings. */
class DbPropertiesTest {
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
    }
}
