package com.subhrodip.pennywise.expensecore.sync.api

/** API representation of one synchronization change. */
data class SyncChangeResponse(val revision: Long, val entityId: String, val deleted: Boolean, val payload: String?)
