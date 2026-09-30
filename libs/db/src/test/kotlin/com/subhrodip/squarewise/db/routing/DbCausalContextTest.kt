package com.subhrodip.squarewise.db.routing

import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies causal thread-local state is scoped and restored across nesting. */
class DbCausalContextTest {
    @Test
    fun `null watermark clears state and nested scope restores the outer value`() {
        assertEquals(null, DbCausalContext.requiredWatermark())

        DbCausalContext.withRequiredWatermark("0/10") {
            assertEquals("0/10", DbCausalContext.requiredWatermark())
            DbCausalContext.withRequiredWatermark(null) {
                assertEquals(null, DbCausalContext.requiredWatermark())
            }
            assertEquals("0/10", DbCausalContext.requiredWatermark())
        }

        assertEquals(null, DbCausalContext.requiredWatermark())
    }
}
