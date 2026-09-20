package com.subhrodip.pennywise.notifications.delivery

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class InboxDeduplicatorTest {
    @Test
    fun `accepts first delivery and rejects duplicate`() {
        val inbox = InboxDeduplicator()
        val eventId = UUID.randomUUID()
        assertTrue(inbox.firstDelivery(eventId))
        assertFalse(inbox.firstDelivery(eventId))
    }
}
