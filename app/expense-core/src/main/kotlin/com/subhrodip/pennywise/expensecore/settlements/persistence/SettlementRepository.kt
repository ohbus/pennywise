package com.subhrodip.pennywise.expensecore.settlements.persistence

import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for persisting and querying [SettlementEntity] records.
 */
@Repository
interface SettlementRepository : JpaRepository<SettlementEntity, UUID> {

    /**
     * Finds a settlement entity by its settlement UUID and owning group UUID.
     *
     * @param settlementId the UUID of the settlement
     * @param groupId the UUID of the group
     * @return the settlement entity if found, or null otherwise
     */
    fun findBySettlementIdAndGroupId(settlementId: UUID, groupId: UUID): SettlementEntity?

    /**
     * Acquires a pessimistic write lock on the target settlement entity by settlement ID and group ID.
     *
     * @param settlementId the UUID of the settlement
     * @param groupId the UUID of the group
     * @return the locked settlement entity if found, or null otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select settlement from SettlementEntity settlement
        where settlement.settlementId = :settlementId and settlement.groupId = :groupId
        """
    )
    fun findForUpdate(
        @Param("settlementId") settlementId: UUID,
        @Param("groupId") groupId: UUID
    ): SettlementEntity?
}
