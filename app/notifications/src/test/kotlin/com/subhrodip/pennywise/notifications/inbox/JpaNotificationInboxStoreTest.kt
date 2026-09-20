package com.subhrodip.pennywise.notifications.inbox

import com.subhrodip.pennywise.notifications.consumer.ProcessedNotificationEventRepository
import com.subhrodip.pennywise.notifications.delivery.JpaEventDeduplicator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:notification_inbox;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
    ]
)
class JpaNotificationInboxStoreTest @Autowired constructor(
    private val store: JpaNotificationInboxStore,
    private val deduplicator: JpaEventDeduplicator,
    private val inboxRepository: NotificationInboxRepository,
    private val eventRepository: ProcessedNotificationEventRepository
) {
    @Test
    fun `persists subject-scoped inbox items in stable cursor order`() {
        inboxRepository.deleteAll()
        val firstId = UUID.fromString("00000000-0000-7000-8000-000000000001")
        val secondId = UUID.fromString("00000000-0000-7000-8000-000000000002")
        val occurredAt = Instant.parse("2026-09-17T12:00:00Z")

        store.append("alice", InboxItem(firstId, "expense.created", "First", occurredAt))
        store.append("alice", InboxItem(secondId, "expense.updated", "Second", occurredAt))
        store.append("bob", InboxItem(UUID.randomUUID(), "expense.created", "Other", occurredAt))

        val inbox = NotificationInbox(store)
        assertEquals(listOf(secondId), inbox.page("alice", null, 1).items.map(InboxItem::notificationId))
        val firstPage = inbox.page("alice", null, 1)
        assertEquals(listOf(firstId), inbox.page("alice", firstPage.nextCursor, 1).items.map(InboxItem::notificationId))
        assertEquals(1, inbox.list("bob").size)
    }

    @Test
    fun `persists event id deduplication across adapter calls`() {
        eventRepository.deleteAll()
        val eventId = UUID.fromString("00000000-0000-7000-8000-000000000003")

        assertTrue(deduplicator.firstDelivery(eventId))
        assertFalse(deduplicator.firstDelivery(eventId))
        assertEquals(1, eventRepository.count())
    }

    @Test
    fun `accepts one event when duplicate deliveries race`() {
        eventRepository.deleteAll()
        val eventId = UUID.fromString("00000000-0000-7000-8000-000000000004")
        val executor = Executors.newFixedThreadPool(2)

        try {
            val outcomes = executor.invokeAll(
                listOf(
                    Callable { deduplicator.firstDelivery(eventId) },
                    Callable { deduplicator.firstDelivery(eventId) }
                )
            ).map { it.get() }

            assertEquals(1, outcomes.count { it })
            assertEquals(1, eventRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `marks notification as read for matching subject`() {
        inboxRepository.deleteAll()
        val notificationId = UUID.fromString("00000000-0000-7000-8000-000000000005")
        val occurredAt = Instant.parse("2026-09-17T12:00:00Z")

        store.append("alice", InboxItem(notificationId, "expense.created", "Unread item", occurredAt, read = false))

        assertFalse(store.list("alice").first().read)

        val wrongSubjectMarked = store.markAsRead("bob", notificationId)
        assertFalse(wrongSubjectMarked)
        assertFalse(store.list("alice").first().read)

        val unknownMarked = store.markAsRead("alice", UUID.randomUUID())
        assertFalse(unknownMarked)

        val marked = store.markAsRead("alice", notificationId)
        assertTrue(marked)
        assertTrue(store.list("alice").first().read)

        val entity = inboxRepository.findById(notificationId).orElseThrow()
        assertTrue(entity.read)
    }
}
