package com.subhrodip.pennywise.expensecore.sync

import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [SyncChangeEntity].
 *
 * Provides query methods to determine max revisions and fetch change journals.
 */
@Repository
interface SyncChangeRepository : JpaRepository<SyncChangeEntity, UUID> {
    /**
     * Finds the maximum revision number currently recorded for a given group.
     *
     * @param groupId the group identifier
     * @return current max revision, or 0 if no records exist
     */
    @Query("SELECT COALESCE(MAX(c.revision), 0) FROM SyncChangeEntity c WHERE c.groupId = :groupId")
    fun findMaxRevision(@Param("groupId") groupId: String): Long

    /**
     * Finds changes recorded after a given revision for a group, sorted ascending by revision.
     *
     * @param groupId the group identifier
     * @param afterRevision the revision threshold
     * @param pageable page request limiting results
     * @return list of matching [SyncChangeEntity]
     */
    @Query(
        """
        SELECT c FROM SyncChangeEntity c
        WHERE c.groupId = :groupId AND c.revision > :afterRevision
        ORDER BY c.revision ASC
        """
    )
    fun findChangesAfter(
        @Param("groupId") groupId: String,
        @Param("afterRevision") afterRevision: Long,
        pageable: Pageable
    ): List<SyncChangeEntity>

    /**
     * Counts remaining changes after a given revision for a group.
     *
     * @param groupId the group identifier
     * @param afterRevision the revision threshold
     * @return total number of changes after the given revision
     */
    @Query(
        """
        SELECT COUNT(c) FROM SyncChangeEntity c
        WHERE c.groupId = :groupId AND c.revision > :afterRevision
        """
    )
    fun countChangesAfter(
        @Param("groupId") groupId: String,
        @Param("afterRevision") afterRevision: Long
    ): Long
}
