package com.subhrodip.pennywise.expensecore.sync.domain

/** One append-only synchronization change. */
data class SyncChange(val revision: Long, val entityId: String, val deleted: Boolean, val payload: String?)
