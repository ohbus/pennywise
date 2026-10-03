package com.subhrodip.squarewise.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.subhrodip.squarewise.notifications.consumer.persistence.ProcessedNotificationEventEntity;
import com.subhrodip.squarewise.notifications.email.smtp.SimpleMailMessage;
import com.subhrodip.squarewise.notifications.inbox.persistence.NotificationInboxEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies the JVM bean and SMTP message surfaces used by notification infrastructure. */
class NotificationJavaBeanCompatibilityTest {
    @Test
    void notification_entities_and_message_expose_their_bound_properties() {
        Instant now = Instant.parse("2026-10-03T00:00:00Z");
        UUID notificationId = UUID.randomUUID();
        NotificationInboxEntity inbox = new NotificationInboxEntity(notificationId, "alice", "expense.created", "Dinner", now, false);
        inbox.setSubject("bob");
        inbox.setEventType("expense.updated");
        inbox.setMessage("Updated dinner");
        inbox.setOccurredAt(now);
        inbox.setRead(true);
        assertEquals(notificationId, inbox.getNotificationId());
        assertEquals("bob", inbox.getSubject());
        assertEquals("expense.updated", inbox.getEventType());
        assertEquals("Updated dinner", inbox.getMessage());
        assertEquals(now, inbox.getOccurredAt());
        assertEquals(true, inbox.getRead());

        UUID eventId = UUID.randomUUID();
        ProcessedNotificationEventEntity processed = new ProcessedNotificationEventEntity(eventId, now);
        processed.setEventId(notificationId);
        processed.setProcessedAt(now);
        assertEquals(notificationId, processed.getEventId());
        assertEquals(now, processed.getProcessedAt());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("notifications@example.test");
        message.setTo("alice@example.test");
        message.setSubject("Expense update");
        message.setBody("Dinner updated");
        assertEquals("notifications@example.test", message.getFrom());
        assertEquals("alice@example.test", message.getRecipient());
        assertEquals("Expense update", message.getSubject());
        assertEquals("Dinner updated", message.getBody());
        assertEquals(message, new SimpleMailMessage("notifications@example.test", new String[] {"alice@example.test"}, "Expense update", "Dinner updated"));
    }
}
