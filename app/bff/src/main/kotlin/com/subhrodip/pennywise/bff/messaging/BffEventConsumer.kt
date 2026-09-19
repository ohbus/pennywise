package com.subhrodip.pennywise.bff.messaging

import com.subhrodip.pennywise.bff.GroupInvalidation
import com.subhrodip.pennywise.bff.LiveUpdate
import com.subhrodip.pennywise.bff.LiveUpdateFanout
import org.slf4j.LoggerFactory

/**
 * Processes incoming committed group events from Expense Core.
 *
 * Deduplicates events by [BffEventEnvelope.eventId], then emits an invalidation event
 * to reactive subscription sinks and publishes a live update to subscriber queues.
 */
class BffEventConsumer(
    private val fanout: LiveUpdateFanout,
    private val deduplicator: BffEventDeduplicator = BffEventDeduplicator()
) {
    private val log = LoggerFactory.getLogger(BffEventConsumer::class.java)

    /**
     * Consumes an event envelope.
     *
     * @param envelope the event envelope to process
     * @return [ConsumptionResult] indicating whether the event was processed, ignored as duplicate, or rejected
     */
    fun consume(envelope: BffEventEnvelope): ConsumptionResult {
        if (deduplicator.isDuplicateAndMark(envelope.eventId)) {
            log.debug("Ignoring duplicate event {}", envelope.eventId)
            return ConsumptionResult.Duplicate(envelope.eventId)
        }

        val groupIdStr = envelope.groupId.toString()
        val changeIdStr = envelope.eventId.toString()
        val revision = envelope.groupRevision

        val invalidation = fanout.emitInvalidation(groupIdStr, revision, changeIdStr)
        val deliveredQueues = fanout.publish(LiveUpdate(groupIdStr, revision))

        log.debug(
            "Processed group event {} for group {}, revision {}, delivered to {} queue(s)",
            envelope.eventId,
            groupIdStr,
            revision,
            deliveredQueues
        )

        return ConsumptionResult.Processed(invalidation, deliveredQueues)
    }
}

sealed interface ConsumptionResult {
    data class Processed(val invalidation: GroupInvalidation, val deliveredQueues: Int) : ConsumptionResult
    data class Duplicate(val eventId: java.util.UUID) : ConsumptionResult
}
