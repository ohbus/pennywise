package com.subhrodip.pennywise.expensecore.settlements

import com.subhrodip.pennywise.errors.ApplicationException
import com.subhrodip.pennywise.errors.ErrorCode
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [SettlementStore] used for unit testing.
 */
@Service
class InMemorySettlementStore : SettlementStore {
    private val settlements = ConcurrentHashMap<Pair<UUID, UUID>, Settlement>()

    override fun record(groupId: UUID, settlement: Settlement): Settlement =
        settlements.computeIfAbsent(groupId to settlement.id) { settlement }

    @Synchronized
    override fun reverse(groupId: UUID, settlementId: UUID, reason: String): Settlement {
        val key = groupId to settlementId
        val settlement = settlements[key]
            ?: throw ApplicationException(ErrorCode.ERR_05, "Settlement not found")
        if (settlement.status == SettlementStatus.REVERSED) return settlement
        return settlement.copy(reason = reason, status = SettlementStatus.REVERSED).also {
            settlements[key] = it
        }
    }
}
