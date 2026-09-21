package com.subhrodip.pennywise.expensecore.groups

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for persisting and querying [GroupAuditEntity] records.
 */
@Repository
interface GroupAuditRepository : JpaRepository<GroupAuditEntity, UUID>, GroupAuditCommandStore
