package com.subhrodip.squarewise.ids

import com.subhrodip.squarewise.ids.events.EventConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Protects the non-blank and uniqueness invariants of shared event metadata. */
class EventConstantsTest {
    /** Ensures the envelope version and every shared header name remain usable. */
    @Test
    fun `event schema metadata is non blank and versioned`() {
        assertEquals(1, EventConstants.CURRENT_SCHEMA_VERSION)

        val headers = listOf(
            EventConstants.Headers.EVENT_ID,
            EventConstants.Headers.EVENT_TYPE,
            EventConstants.Headers.SCHEMA_VERSION,
            EventConstants.Headers.AGGREGATE_ID,
            EventConstants.Headers.GROUP_ID,
            EventConstants.Headers.GROUP_REVISION,
            EventConstants.Headers.OCCURRED_AT
        )

        assertTrue(headers.all(String::isNotBlank))
        assertEquals(headers.size, headers.toSet().size)
    }

    /** Ensures subscriber routing keys are non-blank and do not shadow one another. */
    @Test
    fun `routing keys are distinct and non blank`() {
        val routingKeys = listOf(
            EventConstants.Routing.ALL_EVENTS,
            EventConstants.Routing.ALL_GROUP_EVENTS,
            EventConstants.Routing.ALL_EXPENSE_EVENTS,
            EventConstants.Routing.ALL_SETTLEMENT_EVENTS,
            EventConstants.Routing.AUTH_EMAIL_REQUESTED
        )

        assertFalse(routingKeys.any(String::isBlank))
        assertEquals(routingKeys.size, routingKeys.toSet().size)
    }
}
