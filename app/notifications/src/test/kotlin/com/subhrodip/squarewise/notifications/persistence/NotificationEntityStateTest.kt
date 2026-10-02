package com.subhrodip.squarewise.notifications.persistence

import com.subhrodip.squarewise.notifications.inbox.persistence.NotificationInboxEntity
import com.subhrodip.squarewise.notifications.preferences.persistence.NotificationPreferenceEntity
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Verifies mutable persistence state used by notification preference and inbox workflows. */
class NotificationEntityStateTest {
    @Test
    fun `preference state retains delivery choices and optimistic version`() {
        val preference = NotificationPreferenceEntity(
            subject = "alice",
            emailEnabled = true,
            pushEnabled = false
        )

        preference.emailEnabled = false
        preference.pushEnabled = true
        preference.version = 3

        assertEquals("alice", preference.subject)
        assertEquals(false, preference.emailEnabled)
        assertEquals(true, preference.pushEnabled)
        assertEquals(3, preference.version)
    }

    @Test
    fun `inbox state transitions from unread to read without changing notification identity`() {
        val notificationId = UUID.randomUUID()
        val occurredAt = Instant.parse("2026-10-02T12:00:00Z")
        val notification = NotificationInboxEntity(
            notificationId = notificationId,
            subject = "alice",
            eventType = "expense.created",
            message = "Expense recorded",
            occurredAt = occurredAt
        )

        assertEquals(false, notification.read)
        notification.read = true

        assertEquals(notificationId, notification.notificationId)
        assertEquals("alice", notification.subject)
        assertEquals("expense.created", notification.eventType)
        assertEquals("Expense recorded", notification.message)
        assertEquals(occurredAt, notification.occurredAt)
        assertEquals(true, notification.read)
    }
}
