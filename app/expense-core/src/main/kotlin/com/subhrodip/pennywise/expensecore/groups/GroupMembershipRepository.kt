package com.subhrodip.pennywise.expensecore.groups

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for managing persistence operations on [GroupMembershipEntity].
 */
@Repository
interface GroupMembershipRepository : JpaRepository<GroupMembershipEntity, UUID> {

    /**
     * Checks whether a specific user subject is a member of the given group.
     *
     * @param groupId the UUID of the group
     * @param subject the subject identifier of the user
     * @return true if the membership exists, false otherwise
     */
    fun existsByGroupIdAndSubject(groupId: UUID, subject: String): Boolean

    /**
     * Finds all group memberships for a specific user subject, ordered by membership ID.
     *
     * @param subject the subject identifier of the user
     * @return list of group memberships for the user
     */
    fun findAllBySubjectOrderByMembershipId(subject: String): List<GroupMembershipEntity>

    /**
     * Finds all member records belonging to the specified group.
     *
     * @param groupId the UUID of the group
     * @return list of memberships associated with the group
     */
    fun findByGroupId(groupId: UUID): List<GroupMembershipEntity>
}
