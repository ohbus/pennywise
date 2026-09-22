package com.subhrodip.pennywise.accounts.auth.delivery.outbox

import com.subhrodip.pennywise.accounts.auth.delivery.model.AuthEmailPublishOutcome
import com.subhrodip.pennywise.accounts.auth.delivery.service.AuthEmailOutboxPublisher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Polls the protected auth-email outbox only when explicitly enabled. */
@Component
@ConditionalOnProperty(prefix = "pennywise.auth-email-outbox", name = ["enabled"], havingValue = "true")
class AuthEmailOutboxRelay(private val publisher: AuthEmailOutboxPublisher) {
    @Scheduled(fixedDelayString = "\${pennywise.auth-email-outbox.poll-delay-ms:1000}")
    fun poll(): AuthEmailPublishOutcome = publisher.publishOne()
}
