package com.subhrodip.pennywise.ids

/**
 * Authoritative central constants for RabbitMQ exchange names, routing keys,
 * and event headers used across all Pennywise services, BFF replicas, and notification workers.
 */
object EventConstants {

    /** Central RabbitMQ exchange for domain events */
    const val EVENTS_EXCHANGE: String = "pennywise.events"

    /** Default schema version for event envelopes conforming to envelope.schema.json */
    const val CURRENT_SCHEMA_VERSION: Int = 1

    /** RabbitMQ message header names conforming to envelope metadata */
    object Headers {
        const val EVENT_ID: String = "event-id"
        const val EVENT_TYPE: String = "event-type"
        const val SCHEMA_VERSION: String = "schema-version"
        const val AGGREGATE_ID: String = "aggregate-id"
        const val GROUP_ID: String = "group-id"
        const val GROUP_REVISION: String = "group-revision"
        const val OCCURRED_AT: String = "occurred-at"
    }

    /** Wildcard routing keys for subscribers */
    object Routing {
        const val ALL_EVENTS: String = "#"
        const val ALL_GROUP_EVENTS: String = "group.#"
        const val ALL_EXPENSE_EVENTS: String = "expense.#"
        const val ALL_SETTLEMENT_EVENTS: String = "settlement.#"
    }
}
