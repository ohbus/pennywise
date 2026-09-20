package com.subhrodip.pennywise.accounts.auth.delivery

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

/** Persistence port for Accounts-owned authentication-email delivery records. */
interface AuthEmailOutboxRepository : JpaRepository<AuthEmailOutboxEntity, UUID> {
    /** Finds a delivery record by its idempotent event identifier. */
    fun findByEventId(eventId: UUID): AuthEmailOutboxEntity?
}
