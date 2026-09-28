@file:Suppress("CanConvertToMultiDollarString")

package com.subhrodip.squarewise.accounts.auth.delivery.outbox

import com.subhrodip.squarewise.accounts.auth.delivery.model.AuthEmailPublishOutcome
import com.subhrodip.squarewise.accounts.auth.delivery.service.AuthEmailOutboxPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Polls the protected auth-email outbox only when explicitly enabled. */
@Component
@ConditionalOnProperty(prefix = "squarewise.auth-email-outbox", name = ["enabled"], havingValue = "true")
class AuthEmailOutboxRelay(private val publisher: AuthEmailOutboxPublisher) {
    @Scheduled(fixedDelayString = "\${squarewise.auth-email-outbox.poll-delay-ms:1000}")
    fun poll(): AuthEmailPublishOutcome = publisher.publishOne()
}
