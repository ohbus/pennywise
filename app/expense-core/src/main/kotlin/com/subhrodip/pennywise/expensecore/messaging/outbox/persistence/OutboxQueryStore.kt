package com.subhrodip.pennywise.expensecore.messaging.outbox.persistence
import com.subhrodip.pennywise.expensecore.messaging.outbox.model.OutboxMessage
/** Read-only outbox inspection port. */
interface OutboxQueryStore { fun snapshot(): List<OutboxMessage> }
