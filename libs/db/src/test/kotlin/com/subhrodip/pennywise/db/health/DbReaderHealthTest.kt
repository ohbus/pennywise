package com.subhrodip.pennywise.db.health

import com.subhrodip.pennywise.db.routing.DbExecutionContext
import com.subhrodip.pennywise.db.routing.DbOperationKind
import com.subhrodip.pennywise.db.routing.ReadConsistency
import com.subhrodip.pennywise.db.routing.DbWatermark
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class DbReaderHealthTest {
    private val query = DbExecutionContext("expense.search", DbOperationKind.QUERY, ReadConsistency.EVENTUAL, readerEligible = true)
    private val strongQuery = query.copy(consistency = ReadConsistency.STRONG)

    @Test
    fun `fails closed after repeated reader failures`() {
        val health = DbReaderHealth(failureThreshold = 2, openDuration = Duration.ofSeconds(10))
        health.register("search")
        health.markFailure("search")
        assertEquals(DbReaderDecision.Fail, health.route(query, "search"))
        health.markFailure("search")
        assertEquals(DbReaderState.OPEN, health.state("search"))
        assertEquals(DbReaderDecision.Fail, health.route(query, "search"))
        assertEquals(DbReaderDecision.Writer, health.route(strongQuery, "search"))
    }

    @Test
    fun `recovery closes circuit and allows reader`() {
        val health = DbReaderHealth(failureThreshold = 1)
        health.markFailure("search")
        health.markHealthy("search")
        assertEquals(DbReaderState.HEALTHY, health.state("search"))
        assertEquals(DbReaderDecision.Reader, health.route(query, "search"))
    }

    @Test
    fun `reader cannot answer before required causal watermark`() {
        val health = DbReaderHealth()
        health.markHealthy("search", DbWatermark.parse("0/10"))
        val causal = query.copy(requiredWatermark = "0/20")
        assertEquals(DbReaderDecision.Fail, health.route(causal, "search"))
        health.markHealthy("search", DbWatermark.parse("0/20"))
        assertEquals(DbReaderDecision.Reader, health.route(causal, "search"))
    }

}
