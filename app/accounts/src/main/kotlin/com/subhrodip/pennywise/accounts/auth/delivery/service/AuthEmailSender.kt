package com.subhrodip.pennywise.accounts.auth.delivery.service

import com.subhrodip.pennywise.accounts.auth.delivery.model.AuthEmailMessage
import com.subhrodip.pennywise.accounts.auth.delivery.model.AuthEmailDeliveryResult
/** Outbound port for delivering Pennywise-owned authentication messages. */
fun interface AuthEmailSender {
    /**
     * Queues one authentication message without exposing delivery details to auth policy.
     *
     * @param message template and recipient data; raw credential is delivery-only.
     * @return generic delivery result; it must not disclose account existence.
     */
    fun send(message: AuthEmailMessage): AuthEmailDeliveryResult
}
