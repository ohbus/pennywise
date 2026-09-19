package com.subhrodip.pennywise.notifications.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import java.nio.charset.StandardCharsets

data class SimpleMailMessage(
    var from: String? = null,
    var to: Array<String>? = null,
    var subject: String? = null,
    var text: String? = null
) {
    fun setTo(recipient: String) {
        this.to = arrayOf(recipient)
    }

    var recipient: String?
        get() = to?.firstOrNull()
        set(value) {
            to = if (value != null) arrayOf(value) else null
        }

    var body: String?
        get() = text
        set(value) {
            text = value
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SimpleMailMessage
        if (from != other.from) return false
        if (to != null) {
            if (other.to == null || !to.contentEquals(other.to)) return false
        } else if (other.to != null) return false
        if (subject != other.subject) return false
        if (text != other.text) return false
        return true
    }

    override fun hashCode(): Int {
        var result = from?.hashCode() ?: 0
        result = 31 * result + (to?.contentHashCode() ?: 0)
        result = 31 * result + (subject?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "SimpleMailMessage(from=$from, to=${to?.contentToString()}, subject=$subject, text=$text)"
}

open class MailException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class MailSendException(message: String, cause: Throwable? = null) : MailException(message, cause)

fun interface JavaMailSender {
    @Throws(MailException::class)
    fun send(message: SimpleMailMessage)
}

/**
 * Default SMTP adapter implementing JavaMailSender.
 * Connects directly to SMTP host:port (e.g. local Mailpit on localhost:1025).
 */
@Component
class SmtpJavaMailSender(private val properties: EmailProperties) : JavaMailSender {

    private val log = LoggerFactory.getLogger(SmtpJavaMailSender::class.java)

    override fun send(message: SimpleMailMessage) {
        val recipients = message.to ?: emptyArray()
        if (recipients.isEmpty()) {
            throw ApplicationException(ErrorCode.ERR_02, "At least one recipient must be specified")
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(properties.host, properties.port), 5000)
                socket.soTimeout = 5000

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)

                fun readResponse(expectedPrefix: String) {
                    val line = reader.readLine() ?: throw MailSendException("Premature end of stream from SMTP server")
                    if (!line.startsWith(expectedPrefix)) {
                        throw MailSendException("Unexpected SMTP response: $line (expected prefix $expectedPrefix)")
                    }
                }

                fun sendCommand(cmd: String, expectedPrefix: String) {
                    writer.print(cmd + "\r\n")
                    writer.flush()
                    readResponse(expectedPrefix)
                }

                // Initial 220 banner
                readResponse("220")

                // HELO
                sendCommand("HELO localhost", "250")

                // MAIL FROM
                val from = message.from ?: properties.fromAddress
                sendCommand("MAIL FROM:<$from>", "250")

                // RCPT TO
                for (rcpt in recipients) {
                    sendCommand("RCPT TO:<$rcpt>", "250")
                }

                // DATA
                sendCommand("DATA", "354")

                // Message payload
                writer.print("From: $from\r\n")
                writer.print("To: ${recipients.joinToString(", ")}\r\n")
                writer.print("Subject: ${message.subject ?: ""}\r\n")
                writer.print("Content-Type: text/plain; charset=UTF-8\r\n")
                writer.print("\r\n")
                writer.print((message.text ?: "") + "\r\n")
                writer.print(".\r\n")
                writer.flush()
                readResponse("250")

                // QUIT
                sendCommand("QUIT", "221")
            }
        } catch (e: MailException) {
            throw e
        } catch (e: Exception) {
            throw MailSendException("Failed to send email to ${recipients.contentToString()}: ${e.message}", e)
        }
    }
}
