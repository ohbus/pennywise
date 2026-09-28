package com.subhrodip.pennywise.expensecore.messaging.outbox.model
/** Counts the results of one outbox publication batch. */
data class PublishBatchResult(val claimed: Int, val confirmed: Int, val rejected: Int)
