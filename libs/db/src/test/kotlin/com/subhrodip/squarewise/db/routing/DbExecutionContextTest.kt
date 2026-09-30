package com.subhrodip.squarewise.db.routing

import kotlin.test.Test
import kotlin.test.assertFailsWith

/** Verifies the stable operation-name invariant at the database execution boundary. */
class DbExecutionContextTest {
    @Test
    fun `operation names must be lowercase dot-delimited identifiers`() {
        listOf("", "Expense.Search", "expense search", ".expense", "expense.", "expense..search")
            .forEach { name ->
                assertFailsWith<IllegalArgumentException> {
                    DbExecutionContext(name, DbOperationKind.QUERY)
                }
            }
    }
}
