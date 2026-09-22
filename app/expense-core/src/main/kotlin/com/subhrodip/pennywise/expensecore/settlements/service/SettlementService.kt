package com.subhrodip.pennywise.expensecore.settlements.service
import com.subhrodip.pennywise.expensecore.settlements.domain.Settlement
import com.subhrodip.pennywise.expensecore.settlements.domain.SuggestedSettlement
import com.subhrodip.pennywise.expensecore.settlements.persistence.SettlementStore

import java.util.UUID
import java.nio.charset.StandardCharsets
import org.springframework.stereotype.Service

@Service
class SettlementService(
    private val store: SettlementStore,
    private val suggestionEngine: SettlementSuggestionEngine? = null
) {
    fun record(groupId: UUID, id: UUID, from: UUID, to: UUID, amountMinor: Long, actorSubject: String? = null, idempotencyKey: String? = null): Settlement {
        require(from != to) { "Participants must differ" }
        require(amountMinor > 0) { "Repayment must be positive" }
        if (actorSubject == null && idempotencyKey == null) return store.record(groupId, Settlement(id, from, to, amountMinor))
        require(!actorSubject.isNullOrBlank()) { "Authenticated subject is required" }
        require(idempotencyKey != null && idempotencyKey.length in 16..200) { "Idempotency key must be between 16 and 200 characters" }
        val durableId = UUID.nameUUIDFromBytes("settlement|$groupId|$actorSubject|$idempotencyKey".toByteArray(StandardCharsets.UTF_8))
        return store.record(groupId, Settlement(durableId, from, to, amountMinor))
    }

    fun reverse(groupId: UUID, id: UUID, reason: String): Settlement {
        require(reason.isNotBlank()) { "Reversal reason is required" }
        return store.reverse(groupId, id, reason)
    }

    fun suggestions(groupId: UUID): List<SuggestedSettlement> =
        suggestionEngine?.suggestSettlements(groupId) ?: emptyList()
}
