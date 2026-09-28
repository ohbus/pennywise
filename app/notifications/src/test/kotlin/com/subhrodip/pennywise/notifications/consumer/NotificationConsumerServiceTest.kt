package com.subhrodip.pennywise.notifications.consumer

import com.subhrodip.pennywise.notifications.consumer.model.NotificationConsumptionOutcome
import com.subhrodip.pennywise.notifications.consumer.model.NotificationEvent
import com.subhrodip.pennywise.notifications.consumer.persistence.ProcessedNotificationEventRepository
import com.subhrodip.pennywise.notifications.consumer.service.NotificationConsumer
import com.subhrodip.pennywise.notifications.consumer.service.NotificationConsumerService
import com.subhrodip.pennywise.notifications.consumer.service.NotificationEventConsumer
import com.subhrodip.pennywise.notifications.consumer.service.TransactionalNotificationEventProcessor
import com.subhrodip.pennywise.notifications.consumer.transport.BrokerEnvelopeParser
import com.subhrodip.pennywise.notifications.consumer.transport.RabbitNotificationListener
import com.subhrodip.pennywise.notifications.delivery.rate.TestDeliveryRateLimiter
import com.subhrodip.pennywise.notifications.preferences.persistence.PreferenceStore

import com.subhrodip.pennywise.notifications.email.delivery.EmailDeliveryOutcome
import com.subhrodip.pennywise.notifications.email.delivery.EmailDispatcher

import com.subhrodip.pennywise.notifications.delivery.rate.DeliveryRateLimiter
import com.subhrodip.pennywise.notifications.preferences.model.NotificationPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.time.Duration
import java.time.Instant
import java.util.UUID

class NotificationConsumerServiceTest {

    private lateinit var processor: TransactionalNotificationEventProcessor
    private lateinit var processedEvents: ProcessedNotificationEventRepository
    private lateinit var preferenceStore: PreferenceStore
    private lateinit var emailDispatcher: EmailDispatcher
    private lateinit var deliveryRateLimiter: DeliveryRateLimiter
    private lateinit var consumer: NotificationConsumerService

    @BeforeEach
    fun setUp() {
        processor = mock(TransactionalNotificationEventProcessor::class.java)
        processedEvents = mock(ProcessedNotificationEventRepository::class.java)
        preferenceStore = mock(PreferenceStore::class.java)
        emailDispatcher = mock(EmailDispatcher::class.java)
        deliveryRateLimiter = TestDeliveryRateLimiter(10, Duration.ofMinutes(1))

        consumer = NotificationConsumerService(
            processor = processor,
            processedEvents = processedEvents,
            preferenceStore = preferenceStore,
            emailDispatcher = emailDispatcher,
            deliveryRateLimiter = deliveryRateLimiter
        )
    }

    @Test
    fun `suppresses delivery when only an unverified subject is available`() {
        val event = sampleEvent(subject = "alice")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = true, pushEnabled = true)).`when`(preferenceStore).get("alice")

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, never()).send(anyString(), anyString(), anyString())
    }

    @Test
    fun `dispatches email using explicit recipientEmail if provided`() {
        val event = sampleEvent(
            subject = "alice",
            recipientEmail = "custom.alice@example.com",
            title = "custom.title",
            body = "custom.body"
        )
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = true, pushEnabled = true)).`when`(preferenceStore).get("alice")
        doReturn(EmailDeliveryOutcome.DELIVERED).`when`(emailDispatcher)
            .send("custom.alice@example.com", "Notification: custom.title", "custom.body")

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, times(1))
            .send("custom.alice@example.com", "Notification: custom.title", "custom.body")
    }

    @Test
    fun `dispatches email using subject directly if subject contains email address`() {
        val event = sampleEvent(subject = "bob@example.com")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = true)).`when`(preferenceStore).get("bob@example.com")
        doReturn(EmailDeliveryOutcome.DELIVERED).`when`(emailDispatcher)
            .send("bob@example.com", "Notification: expense.created", "Dinner was added")

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, times(1))
            .send("bob@example.com", "Notification: expense.created", "Dinner was added")
    }

    @Test
    fun `skips email dispatch when recipient preferences have email disabled`() {
        val event = sampleEvent(subject = "bob")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = false, pushEnabled = true)).`when`(preferenceStore).get("bob")

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, never()).send(anyString(), anyString(), anyString())
    }

    @Test
    fun `suppresses email when recipient preference is missing`() {
        val event = sampleEvent(subject = "charlie")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(null).`when`(preferenceStore).get("charlie")
        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, never()).send(anyString(), anyString(), anyString())
    }

    @Test
    fun `suppresses email when preference store throws an exception`() {
        val event = sampleEvent(subject = "dave")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doThrow(RuntimeException("Preferences database connection failure")).`when`(preferenceStore).get("dave")
        val outcome = consumer.consume(event)
        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(emailDispatcher, never()).send(anyString(), anyString(), anyString())
    }

    @Test
    fun `suppresses delivery after the per-recipient rate limit`() {
        deliveryRateLimiter = TestDeliveryRateLimiter(1, Duration.ofMinutes(1))
        consumer = NotificationConsumerService(processor, processedEvents, preferenceStore, emailDispatcher, deliveryRateLimiter)
        val first = sampleEvent(subject = "rate@example.com")
        val second = sampleEvent(subject = "rate@example.com")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(first)
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(second)
        doReturn(NotificationPreferences(emailEnabled = true)).`when`(preferenceStore).get("rate@example.com")
        doReturn(EmailDeliveryOutcome.DELIVERED).`when`(emailDispatcher)
            .send("rate@example.com", "Notification: expense.created", "Dinner was added")

        consumer.consume(first)
        consumer.consume(second)

        verify(emailDispatcher, times(1))
            .send("rate@example.com", "Notification: expense.created", "Dinner was added")
    }

    @Test
    fun `email dispatch failure does not fail inbox consumption or throw exception`() {
        val event = sampleEvent(subject = "eve")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = true)).`when`(preferenceStore).get("eve")
        doThrow(RuntimeException("SMTP server unreachable")).`when`(emailDispatcher)
            .send(anyString(), anyString(), anyString())

        val outcome = consumer.consume(event)
        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(processor, times(1)).process(event)
    }

    @Test
    fun `email dispatch retryable or permanent failure does not fail consumption`() {
        val event = sampleEvent(subject = "frank")
        doReturn(NotificationConsumptionOutcome.APPLIED).`when`(processor).process(event)
        doReturn(NotificationPreferences(emailEnabled = true)).`when`(preferenceStore).get("frank")
        doReturn(EmailDeliveryOutcome.PERMANENT_FAILURE).`when`(emailDispatcher)
            .send(anyString(), anyString(), anyString())

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.APPLIED, outcome)
        verify(processor, times(1)).process(event)
    }

    @Test
    fun `duplicate event does not trigger email dispatch`() {
        val event = sampleEvent(subject = "grace")
        doReturn(NotificationConsumptionOutcome.DUPLICATE).`when`(processor).process(event)

        val outcome = consumer.consume(event)

        assertEquals(NotificationConsumptionOutcome.DUPLICATE, outcome)
        verify(preferenceStore, never()).get(anyString())
        verify(emailDispatcher, never()).send(anyString(), anyString(), anyString())
    }

    private fun sampleEvent(
        subject: String,
        recipientEmail: String? = null,
        title: String = "expense.created",
        body: String = "Dinner was added"
    ) = NotificationEvent(
        eventId = UUID.randomUUID(),
        notificationId = UUID.randomUUID(),
        subject = subject,
        eventType = title,
        message = body,
        occurredAt = Instant.parse("2026-09-18T10:00:00Z"),
        recipientEmail = recipientEmail,
        title = title,
        body = body
    )
}
