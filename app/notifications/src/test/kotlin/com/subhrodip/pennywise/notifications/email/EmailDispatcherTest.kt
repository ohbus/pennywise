package com.subhrodip.pennywise.notifications.email
import com.subhrodip.pennywise.notifications.email.config.EmailProperties
import com.subhrodip.pennywise.notifications.email.delivery.EmailDeliveryOutcome
import com.subhrodip.pennywise.notifications.email.delivery.EmailDispatcher
import com.subhrodip.pennywise.notifications.email.smtp.JavaMailSender
import com.subhrodip.pennywise.notifications.email.smtp.MailSendException
import com.subhrodip.pennywise.notifications.email.smtp.SimpleMailMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.net.ConnectException
import java.net.SocketException

class EmailDispatcherTest {

    private lateinit var mailSender: JavaMailSender
    private lateinit var properties: EmailProperties
    private lateinit var dispatcher: EmailDispatcher

    @BeforeEach
    fun setUp() {
        mailSender = mock(JavaMailSender::class.java)
        properties = EmailProperties(
            host = "localhost",
            port = 1025,
            fromAddress = "notifications@pennywise.local",
            enabled = true,
            maxAttempts = 3,
            retryDelayMs = 0L
        )
        dispatcher = EmailDispatcher(properties, mailSender)
    }

    @Test
    fun `successful dispatch sends SimpleMailMessage with correct recipient from subject and text`() {
        val recipient = "alice@example.com"
        val subject = "Expense Split Update"
        val body = "You owe $25.00 for Dinner."

        val outcome = dispatcher.dispatch(recipient, subject, body)

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.DELIVERED)

        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender, times(1)).send(captureMessage(messageCaptor))

        val sentMessage = messageCaptor.value
        assertThat(sentMessage.from).isEqualTo("notifications@pennywise.local")
        assertThat(sentMessage.to).containsExactly("alice@example.com")
        assertThat(sentMessage.subject).isEqualTo("Expense Split Update")
        assertThat(sentMessage.text).isEqualTo("You owe $25.00 for Dinner.")
    }

    @Test
    fun `transient mail send exception returns RETRYABLE_FAILURE after exhausting retries`() {
        val recipient = "bob@example.com"
        val subject = "Payment Reminder"
        val body = "Please settle the outstanding balance."

        doThrow(MailSendException("Connection refused", ConnectException("Connection refused to 1025")))
            .`when`(mailSender)
            .send(anyMessage())

        val outcome = dispatcher.dispatch(recipient, subject, body)

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.RETRYABLE_FAILURE)
        verify(mailSender, times(3)).send(anyMessage())
    }

    @Test
    fun `transient socket exception retries and succeeds on subsequent attempt`() {
        val recipient = "charlie@example.com"
        val subject = "Weekly Summary"
        val body = "Here is your weekly split summary."

        var attempts = 0
        doAnswer {
            attempts++
            if (attempts == 1) {
                throw MailSendException("Transient network glitch", SocketException("Connection reset"))
            }
            null
        }.`when`(mailSender).send(anyMessage())

        val outcome = dispatcher.dispatch(recipient, subject, body)

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.DELIVERED)
        assertThat(attempts).isEqualTo(2)
        verify(mailSender, times(2)).send(anyMessage())
    }

    @Test
    fun `disabled dispatcher skips sending and returns SKIPPED`() {
        properties.enabled = false
        val outcome = dispatcher.dispatch("alice@example.com", "Test", "Disabled body")

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.SKIPPED)
        verify(mailSender, never()).send(anyMessage())
    }

    @Test
    fun `invalid email address returns PERMANENT_FAILURE without sending`() {
        val invalidEmails = listOf("", "   ", "not-an-email", "@missinguser.com", "missingdomain@.com")

        for (invalid in invalidEmails) {
            val outcome = dispatcher.dispatch(invalid, "Subject", "Body")
            assertThat(outcome).isEqualTo(EmailDeliveryOutcome.PERMANENT_FAILURE)
        }

        verify(mailSender, never()).send(anyMessage())
    }

    @Test
    fun `blank subject returns PERMANENT_FAILURE`() {
        val outcome = dispatcher.dispatch("alice@example.com", "   ", "Valid body")

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.PERMANENT_FAILURE)
        verify(mailSender, never()).send(anyMessage())
    }

    @Test
    fun `illegal argument exception during sending returns PERMANENT_FAILURE without retrying`() {
        doThrow(IllegalArgumentException("Illegal message header"))
            .`when`(mailSender)
            .send(anyMessage())

        val outcome = dispatcher.dispatch("alice@example.com", "Valid Subject", "Body")

        assertThat(outcome).isEqualTo(EmailDeliveryOutcome.PERMANENT_FAILURE)
        verify(mailSender, times(1)).send(anyMessage())
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyMessage(): SimpleMailMessage = any(SimpleMailMessage::class.java) ?: SimpleMailMessage()

    private fun captureMessage(captor: ArgumentCaptor<SimpleMailMessage>): SimpleMailMessage {
        captor.capture()
        return SimpleMailMessage()
    }
}
