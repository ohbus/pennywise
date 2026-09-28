package com.subhrodip.pennywise.notifications.email.smtp
/** Exception raised when SMTP delivery fails. */
class MailSendException(message: String, cause: Throwable? = null) : MailException(message, cause)
