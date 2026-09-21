package com.subhrodip.pennywise.notifications.consumer

import com.subhrodip.pennywise.notifications.email.EmailDispatcher
import com.subhrodip.pennywise.notifications.email.opaqueRecipientId
import com.subhrodip.pennywise.notifications.delivery.DeliveryRateLimiter
import com.subhrodip.pennywise.notifications.preferences.PreferenceStore

import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

data class NotificationEvent(
    val eventId: UUID,
    val notificationId: UUID,
    val subject: String,
    val eventType: String,
    val message: String,
    val occurredAt: Instant,
    val recipientEmail: String? = null,
    val title: String = eventType,
    val body: String = message
) {
    val recipientId: String get() = subject
}

enum class NotificationConsumptionOutcome {
    APPLIED,
    DUPLICATE
}

fun interface NotificationConsumer {
    fun consume(event: NotificationEvent): NotificationConsumptionOutcome
}

/**
 * Broker-facing application boundary. APPLIED and DUPLICATE deliveries may be
 * acknowledged; any exception is deliberately propagated so the delivery can
 * be retried by the broker.
 *
 * When an event is APPLIED, email dispatch is triggered if allowed by recipient preferences.
 * Email dispatch errors are isolated and never cause inbox storage rollback.
 */
@Service
class NotificationEventConsumer(
    private val processor: TransactionalNotificationEventProcessor,
    private val processedEvents: ProcessedNotificationEventRepository,
    private val preferenceStore: PreferenceStore,
    private val emailDispatcher: EmailDispatcher,
    private val deliveryRateLimiter: DeliveryRateLimiter
) : NotificationConsumer {

    private val log = LoggerFactory.getLogger(NotificationEventConsumer::class.java)

    override fun consume(event: NotificationEvent): NotificationConsumptionOutcome {
        val outcome = try {
            processor.process(event)
        } catch (failure: RuntimeException) {
            if (failure.isConstraintViolation() && processedEvents.existsById(event.eventId)) {
                NotificationConsumptionOutcome.DUPLICATE
            } else {
                throw failure
            }
        }

        if (outcome == NotificationConsumptionOutcome.APPLIED) {
            dispatchEmailIfEnabled(event)
        }

        return outcome
    }

    private fun dispatchEmailIfEnabled(event: NotificationEvent) {
        try {
            val recipientId = event.recipientId
            val preferences = try {
                preferenceStore.get(recipientId)
            } catch (e: Exception) {
                log.warn("Failed to retrieve notification preferences for recipientId={}; suppressing delivery", recipientId, e)
                return
            }

            val emailEnabled = preferences?.emailEnabled ?: false
            if (!emailEnabled) {
                log.info("Email notifications disabled for recipient='{}'; skipping dispatch", recipientId)
                return
            }

            val recipientEmail = resolveRecipientEmail(event)
            if (!deliveryRateLimiter.allow(recipientId)) {
                log.warn("Email delivery rate limit reached for recipientId={}; suppressing delivery", recipientId)
                return
            }
            val subject = "Notification: ${event.title}"
            val body = event.body

            val deliveryOutcome = emailDispatcher.send(recipientEmail, subject, body)
            log.info("Email dispatch outcome for recipientId={} (notificationId={}): {}", opaqueRecipientId(recipientEmail), event.notificationId, deliveryOutcome)
        } catch (t: Throwable) {
            log.error("Unexpected email delivery failure for notification {}, errorClass={}", event.notificationId, t::class.simpleName)
        }
    }

    private fun resolveRecipientEmail(event: NotificationEvent): String {
        if (!event.recipientEmail.isNullOrBlank()) {
            return event.recipientEmail.trim()
        }
        val recipient = event.subject.trim()
        require(recipient.contains("@")) { "Verified recipient email is required for notification delivery" }
        return recipient
    }
}

private fun RuntimeException.isConstraintViolation(): Boolean =
    this is DataIntegrityViolationException || this is ConstraintViolationException
