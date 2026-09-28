package com.subhrodip.pennywise.expensecore.groups.persistence.repository

import com.subhrodip.pennywise.expensecore.groups.persistence.store.GroupAuditCommandStore
import com.subhrodip.pennywise.expensecore.groups.domain.GroupAuditEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for persisting and querying [GroupAuditEntity] records.
 */
@Repository
interface GroupAuditRepository : JpaRepository<GroupAuditEntity, UUID>, GroupAuditCommandStore
