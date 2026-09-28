package com.subhrodip.pennywise.notifications.email.smtp
/** Minimal SMTP-neutral message model used by the mail port. */
data class SimpleMailMessage(var from: String? = null, var to: Array<String>? = null, var subject: String? = null, var text: String? = null) {
    fun setTo(recipient: String) { to = arrayOf(recipient) }
    var recipient: String?
        get() = to?.firstOrNull()
        set(value) { to = if (value != null) arrayOf(value) else null }
    var body: String?
        get() = text
        set(value) { text = value }
    override fun equals(other: Any?): Boolean = other is SimpleMailMessage && from == other.from && (to?.contentEquals(other.to ?: emptyArray()) ?: (other.to == null)) && subject == other.subject && text == other.text
    override fun hashCode(): Int = listOf(from, to?.contentHashCode(), subject, text).hashCode()
    override fun toString(): String = "SimpleMailMessage(from=$from, to=${to?.contentToString()}, subject=$subject, text=$text)"
}
