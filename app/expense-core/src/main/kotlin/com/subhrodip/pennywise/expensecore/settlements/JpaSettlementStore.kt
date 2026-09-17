package com.subhrodip.pennywise.expensecore.settlements

import java.util.UUID
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JPA persistence adapter implementing [SettlementStore] to record settlements and execute reversals.
 *
 * Invariants:
 * - Recording a settlement is idempotent if a settlement with the same ID and group ID already exists.
 * - Reversing a settlement acquires a pessimistic write lock and transitions the settlement status to REVERSED.
 */
@Primary
@Service
class JpaSettlementStore(private val repository: SettlementRepository) : SettlementStore {

    /**
     * Records a new settlement or returns the existing record if already present.
     *
     * @param groupId the UUID of the group
     * @param settlement the domain settlement data to record
     * @return the persisted or existing domain settlement
     */
    @Transactional
    override fun record(groupId: UUID, settlement: Settlement): Settlement {
        val existing = repository.findBySettlementIdAndGroupId(settlement.id, groupId)
        if (existing != null) return existing.toDomain()
        return repository.save(settlement.toEntity(groupId)).toDomain()
    }

    /**
     * Reverses a settlement with the given reason, acquiring a pessimistic lock.
     *
     * @param groupId the UUID of the group
     * @param settlementId the UUID of the settlement to reverse
     * @param reason explanation for the reversal
     * @return the updated domain settlement with REVERSED status
     * @throws IllegalStateException if the settlement cannot be found
     */
    @Transactional
    override fun reverse(groupId: UUID, settlementId: UUID, reason: String): Settlement {
        val entity = repository.findForUpdate(settlementId, groupId)
            ?: error("Settlement not found")
        if (entity.status == SettlementStatus.REVERSED) return entity.toDomain()
        entity.status = SettlementStatus.REVERSED
        entity.reversalReason = reason
        return repository.save(entity).toDomain()
    }
}

private fun Settlement.toEntity(groupId: UUID) = SettlementEntity(
    settlementId = id,
    groupId = groupId,
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    amountMinor = amountMinor,
    reversalReason = reason,
    status = status
)

private fun SettlementEntity.toDomain() = Settlement(
    id = settlementId,
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    amountMinor = amountMinor,
    reason = reversalReason,
    status = status
)
