package com.subhrodip.pennywise.accounts.auth.delivery

import java.util.UUID
import java.time.Instant
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

/** Persistence port for Accounts-owned authentication-email delivery records. */
interface AuthEmailOutboxRepository : JpaRepository<AuthEmailOutboxEntity, UUID> {
    /** Finds a delivery record by its idempotent event identifier. */
    fun findByEventId(eventId: UUID): AuthEmailOutboxEntity?

    /** Finds one available delivery while holding a database row lock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select outbox from AuthEmailOutboxEntity outbox
         where (outbox.status = 'PENDING' or
                (outbox.status = 'CLAIMED' and outbox.availableAt <= :now))
           and outbox.availableAt <= :now
         order by outbox.createdAt asc
        """
    )
    fun findAvailableForClaim(@org.springframework.data.repository.query.Param("now") now: Instant): List<AuthEmailOutboxEntity>
}
