package com.subhrodip.squarewise.accounts.requests.deletion.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for [AccountDeletionRequestEntity].
 */
@Repository
interface DeletionRequestRepository : JpaRepository<AccountDeletionRequestEntity, String>
