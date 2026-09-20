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

    /** Acknowledges only the currently claimed event. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update AuthEmailOutboxEntity e set e.status = 'PUBLISHED', e.availableAt = :now where e.eventId = :eventId and e.status = 'CLAIMED'")
    fun acknowledge(@org.springframework.data.repository.query.Param("eventId") eventId: UUID, @org.springframework.data.repository.query.Param("now") now: Instant): Int

    /** Returns a claimed event to retry or parks it after the attempt limit. */
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update AuthEmailOutboxEntity e set e.status = case when e.attempts >= :maximumAttempts then 'PARKED' else 'PENDING' end, e.availableAt = :availableAt where e.eventId = :eventId and e.status = 'CLAIMED'")
    fun reject(@org.springframework.data.repository.query.Param("eventId") eventId: UUID, @org.springframework.data.repository.query.Param("maximumAttempts") maximumAttempts: Int, @org.springframework.data.repository.query.Param("availableAt") availableAt: Instant): Int
}
