package com.subhrodip.pennywise.expensecore.messaging.outbox.model
/** Lifecycle state of a transactional outbox message. */
enum class OutboxStatus { PENDING, CLAIMED, PUBLISHED, PARKED }
