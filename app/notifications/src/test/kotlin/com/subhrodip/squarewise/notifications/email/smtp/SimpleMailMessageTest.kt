package com.subhrodip.squarewise.notifications.email.smtp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Verifies the SMTP-neutral message value object's array and nullable-field contracts. */
class SimpleMailMessageTest {
    @Test
    fun `recipient and body properties map to the wire fields`() {
        val message = SimpleMailMessage()

        message.setTo("alice@example.com")
        message.body = "hello"

        assertThat(message.recipient).isEqualTo("alice@example.com")
        assertThat(message.to).containsExactly("alice@example.com")
        assertThat(message.body).isEqualTo("hello")
        assertThat(message.text).isEqualTo("hello")

        message.recipient = "bob@example.com"
        assertThat(message.to).containsExactly("bob@example.com")

        message.recipient = null
        message.body = null

        assertThat(message.to).isNull()
        assertThat(message.recipient).isNull()
        assertThat(message.text).isNull()
    }

    @Test
    fun `equality compares array contents and every message field`() {
        val original = SimpleMailMessage(
            from = "sender@example.com",
            to = arrayOf("alice@example.com", "bob@example.com"),
            subject = "Subject",
            text = "Body"
        )
        val same = SimpleMailMessage(
            from = "sender@example.com",
            to = arrayOf("alice@example.com", "bob@example.com"),
            subject = "Subject",
            text = "Body"
        )

        assertThat(original).isEqualTo(same)
        assertThat(original.hashCode()).isEqualTo(same.hashCode())
        assertThat(original.toString()).contains(
            "sender@example.com",
            "alice@example.com",
            "bob@example.com",
            "Subject",
            "Body"
        )

        assertThat(original).isNotEqualTo(original.copy(from = "other@example.com"))
        assertThat(original).isNotEqualTo(original.copy(to = arrayOf("different@example.com")))
        assertThat(original).isNotEqualTo(original.copy(subject = "Different"))
        assertThat(original).isNotEqualTo(original.copy(text = "Different"))
        assertThat(original).isNotEqualTo("not a mail message")
    }

    @Test
    fun `null and empty arrays preserve their own equality contracts`() {
        assertThat(SimpleMailMessage()).isEqualTo(SimpleMailMessage())
        assertThat(SimpleMailMessage(to = emptyArray())).isEqualTo(SimpleMailMessage())
        assertThat(SimpleMailMessage(to = emptyArray())).isEqualTo(SimpleMailMessage(to = emptyArray()))
        assertThat(SimpleMailMessage()).isNotEqualTo(SimpleMailMessage(to = emptyArray()))
    }

    @Test
    fun `empty recipients and null fields retain safe diagnostics and hashes`() {
        val empty = SimpleMailMessage(to = emptyArray())
        assertThat(empty.recipient).isNull()
        assertThat(empty.toString()).contains("to=[]", "subject=null", "text=null")
        assertThat(empty.hashCode()).isEqualTo(SimpleMailMessage(to = emptyArray()).hashCode())
        assertThat(SimpleMailMessage().hashCode()).isEqualTo(SimpleMailMessage().hashCode())
        assertThat(empty).isNotEqualTo(null)
        assertThat(SimpleMailMessage().toString()).contains("to=null")
    }
}
