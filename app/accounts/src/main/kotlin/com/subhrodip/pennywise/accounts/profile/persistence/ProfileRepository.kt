package com.subhrodip.pennywise.accounts.profile.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for [ProfileEntity].
 */
@Repository
interface ProfileRepository : JpaRepository<ProfileEntity, UUID> {
    /**
     * Finds an account profile by its unique OIDC subject identifier.
     *
     * @param subject The OIDC subject string.
     * @return The matching [ProfileEntity], or null if not found.
     */
    fun findBySubject(subject: String): ProfileEntity?
}
