package com.subhrodip.pennywise.expensecore.messaging.config
import com.subhrodip.pennywise.expensecore.messaging.outbox.service.OutboxPublisher
import com.subhrodip.pennywise.expensecore.messaging.outbox.model.PublishBatchResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Background daemon polling available outbox messages and pumping them to the broker.
 * Gated by `pennywise.outbox.enabled=true` so tests and non-worker containers remain isolated.
 */
@Component
@ConditionalOnProperty(prefix = "pennywise.outbox", name = ["enabled"], havingValue = "true")
class OutboxRelayDaemon(
    private val publisher: OutboxPublisher
) {
    @Scheduled(fixedDelayString = "\${pennywise.outbox.poll-delay-ms:1000}")
    fun pollAndPublish(): PublishBatchResult {
        return publisher.publishAvailable()
    }
}
