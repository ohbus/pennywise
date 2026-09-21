package com.subhrodip.pennywise.db.policy

import com.subhrodip.pennywise.db.routing.DbExecutionContext
import com.subhrodip.pennywise.db.routing.DbOperationKind
import com.subhrodip.pennywise.db.routing.DbRoute
import com.subhrodip.pennywise.db.routing.ReadConsistency
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Verifies the first writer-safety boundary independently of Spring. */
class DbRouteGuardTest {
    private val guard = DbRouteGuard()

    @Test
    fun `commands cannot use a reader`() {
        assertFailsWith<IllegalStateException> {
            guard.validate(DbExecutionContext("expense.create", DbOperationKind.COMMAND), DbRoute.READER)
        }
    }

    @Test
    fun `locking queries cannot use a reader`() {
        assertFailsWith<IllegalStateException> {
            guard.validate(DbExecutionContext("group.lock", DbOperationKind.LOCKING_QUERY), DbRoute.READER)
        }
    }

    @Test
    fun `eventual queries may use a reader`() {
        guard.validate(
            DbExecutionContext("expense.search", DbOperationKind.QUERY, ReadConsistency.EVENTUAL),
            DbRoute.READER
        )
        assertIs<DbRoute>(DbRoute.READER)
    }
}
