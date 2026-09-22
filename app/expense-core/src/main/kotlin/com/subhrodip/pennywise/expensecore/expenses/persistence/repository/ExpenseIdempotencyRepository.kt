package com.subhrodip.pennywise.expensecore.expenses.persistence.repository

import com.subhrodip.pennywise.expensecore.expenses.persistence.entity.ExpenseIdempotencyEntity
import java.util.UUID
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/** Repository for durable expense mutation claims. */
@Repository
interface ExpenseIdempotencyRepository : JpaRepository<ExpenseIdempotencyEntity, UUID> {
    /** Finds a prior claim in the actor and group operation scope. */
    fun findByGroupIdAndActorSubjectAndOperationAndIdempotencyKey(
        groupId: UUID,
        actorSubject: String,
        operation: String,
        idempotencyKey: String
    ): ExpenseIdempotencyEntity?

    /** Returns at most one cleanup batch of claims older than [createdBefore]. */
    fun findByCreatedAtBefore(createdBefore: Instant, pageable: Pageable): List<ExpenseIdempotencyEntity>
}
