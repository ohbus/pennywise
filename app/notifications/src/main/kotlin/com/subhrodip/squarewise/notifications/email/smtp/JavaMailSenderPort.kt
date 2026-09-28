package com.subhrodip.squarewise.notifications.email.smtp
/** Port for sending a fully composed email message. */
fun interface JavaMailSender {
    @Throws(MailException::class)
    fun send(message: SimpleMailMessage)
}
