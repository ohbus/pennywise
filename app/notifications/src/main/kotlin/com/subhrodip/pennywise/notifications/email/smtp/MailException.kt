package com.subhrodip.pennywise.notifications.email.smtp
/** Base exception for mail-port failures. */
open class MailException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
