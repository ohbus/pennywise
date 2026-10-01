package com.subhrodip.squarewise.bff.messaging.persistence

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Verifies bounded BFF event deduplication rejects unsafe cache capacities. */
class BffEventDeduplicatorTest {
    /** Verifies zero and negative capacities fail before an unbounded cache can be created. */
    @Test
    fun `rejects non-positive capacity`() {
        assertThrows(IllegalArgumentException::class.java) {
            BffEventDeduplicator(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BffEventDeduplicator(-1)
        }
    }
}
