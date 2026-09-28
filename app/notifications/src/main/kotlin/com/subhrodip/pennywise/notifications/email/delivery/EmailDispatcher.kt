package com.subhrodip.pennywise.notifications.email.delivery
import com.subhrodip.pennywise.notifications.email.config.EmailProperties
import com.subhrodip.pennywise.notifications.email.smtp.JavaMailSender
import com.subhrodip.pennywise.notifications.email.smtp.MailSendException
import com.subhrodip.pennywise.notifications.email.smtp.SimpleMailMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException

@Component
class EmailDispatcher(
    private val properties: EmailProperties,
    private val mailSender: JavaMailSender
) {

    private val log = LoggerFactory.getLogger(EmailDispatcher::class.java)

    fun dispatch(recipient: String, subject: String, body: String): EmailDeliveryOutcome {
        val recipientId = opaqueRecipientId(recipient)
        if (!properties.enabled) {
            log.info("Email dispatch disabled; skipping recipientId={}", recipientId)
            return EmailDeliveryOutcome.SKIPPED
        }

        if (!isValidEmail(recipient)) {
            log.warn("Invalid email address for recipientId={}", recipientId)
            return EmailDeliveryOutcome.PERMANENT_FAILURE
        }

        if (subject.isBlank()) {
            log.warn("Email subject cannot be blank")
            return EmailDeliveryOutcome.PERMANENT_FAILURE
        }

        val message = SimpleMailMessage().apply {
            from = properties.fromAddress
            setTo(recipient.trim())
            this.subject = subject
            text = body
        }

        val maxAttempts = properties.maxAttempts.coerceAtLeast(1)
        for (attempt in 1..maxAttempts) {
            try {
                mailSender.send(message)
                log.info("Successfully dispatched email to recipientId={} on attempt {}", recipientId, attempt)
                return EmailDeliveryOutcome.DELIVERED
            } catch (e: Exception) {
                if (isPermanentFailure(e)) {
                    log.warn("Permanent email delivery failure for recipientId={}, errorClass={}", recipientId, e::class.simpleName)
                    return EmailDeliveryOutcome.PERMANENT_FAILURE
                }

                if (isTransientFailure(e)) {
                    log.warn("Transient email delivery failure for recipientId={} (attempt {}/{})", recipientId, attempt, maxAttempts)
                    if (attempt < maxAttempts) {
                        if (properties.retryDelayMs > 0) {
                            try {
                                Thread.sleep(properties.retryDelayMs)
                            } catch (ie: InterruptedException) {
                                Thread.currentThread().interrupt()
                                return EmailDeliveryOutcome.RETRYABLE_FAILURE
                            }
                        }
                        continue
                    }
                    return EmailDeliveryOutcome.RETRYABLE_FAILURE
                }

                log.error("Non-retryable email delivery failure for recipientId={}, errorClass={}", recipientId, e::class.simpleName)
                return EmailDeliveryOutcome.PERMANENT_FAILURE
            }
        }

        return EmailDeliveryOutcome.RETRYABLE_FAILURE
    }

    fun send(recipient: String, subject: String, body: String): EmailDeliveryOutcome =
        dispatch(recipient, subject, body)

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || trimmed.length > 254) return false
        return EMAIL_REGEX.matches(trimmed)
    }

    private fun isPermanentFailure(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is IllegalArgumentException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun isTransientFailure(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is ConnectException ||
                current is SocketException ||
                current is SocketTimeoutException ||
                current is IOException ||
                current is MailSendException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }
}
