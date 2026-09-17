package com.subhrodip.pennywise.expensecore.settlements

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

enum class SettlementStatus { RECORDED, REVERSED }

@Service
class SettlementService(
    private val store: SettlementStore,
    private val suggestionEngine: SettlementSuggestionEngine? = null
) {
    fun record(groupId: UUID, id: UUID, from: UUID, to: UUID, amountMinor: Long): Settlement {
        require(from != to) { "Participants must differ" }
        require(amountMinor > 0) { "Repayment must be positive" }
        return store.record(groupId, Settlement(id, from, to, amountMinor))
    }

    fun reverse(groupId: UUID, id: UUID, reason: String): Settlement {
        require(reason.isNotBlank()) { "Reversal reason is required" }
        return store.reverse(groupId, id, reason)
    }

    fun suggestions(groupId: UUID): List<SuggestedSettlement> =
        suggestionEngine?.suggestSettlements(groupId) ?: emptyList()
}
