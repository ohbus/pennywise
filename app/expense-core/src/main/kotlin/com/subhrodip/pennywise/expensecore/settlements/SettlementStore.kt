package com.subhrodip.pennywise.expensecore.settlements

import java.util.UUID

/**
 * Domain port defining persistence operations for recorded settlements and reversals.
 */
interface SettlementStore {
    /**
     * Records a new settlement or returns the existing record idempotently.
     */
    fun record(groupId: UUID, settlement: Settlement): Settlement

    /**
     * Atomically reverses a recorded settlement.
     */
    fun reverse(groupId: UUID, settlementId: UUID, reason: String): Settlement
}
