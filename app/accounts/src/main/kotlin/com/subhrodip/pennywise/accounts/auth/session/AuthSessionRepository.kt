package com.subhrodip.pennywise.accounts.auth.session

import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** Persistence port for refresh-token rotation and family revocation. */
interface AuthSessionRepository : JpaRepository<AuthSessionEntity, UUID> {
    /** Revokes one refresh-token family, including descendants and ancestors. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AuthSessionEntity s SET s.revokedAt = :revokedAt WHERE s.familyId = :familyId AND s.revokedAt IS NULL")
    fun revokeFamily(@Param("familyId") familyId: UUID, @Param("revokedAt") revokedAt: Instant): Int

    /** Atomically marks a refresh session replaced and revoked exactly once. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE AuthSessionEntity s
           SET s.revokedAt = :revokedAt, s.replacedBySessionId = :replacement
         WHERE s.sessionId = :sessionId
           AND s.revokedAt IS NULL
           AND s.expiresAt > :revokedAt
        """
    )
    fun rotateIfActive(
        @Param("sessionId") sessionId: UUID,
        @Param("replacement") replacement: UUID,
        @Param("revokedAt") revokedAt: Instant
    ): Int

    /** Finds an auth session by its refresh token HMAC digest. */
    fun findByRefreshTokenDigest(refreshTokenDigest: ByteArray): AuthSessionEntity?
}
