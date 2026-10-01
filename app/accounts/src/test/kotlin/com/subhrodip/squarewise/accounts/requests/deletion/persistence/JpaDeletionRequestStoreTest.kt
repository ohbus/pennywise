package com.subhrodip.squarewise.accounts.requests.deletion.persistence

import com.subhrodip.squarewise.accounts.auth.session.AuthSessionRepository
import com.subhrodip.squarewise.accounts.profile.persistence.ProfileRepository
import com.subhrodip.squarewise.accounts.profile.persistence.ProfileStore
import com.subhrodip.squarewise.accounts.requests.deletion.model.DeletionStatus
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

/** Verifies deletion-request side effects when the profile is not yet resolvable. */
class JpaDeletionRequestStoreTest {
    private val repository = mock(DeletionRequestRepository::class.java)
    private val profileStore = mock(ProfileStore::class.java)
    private val profileRepository = mock(ProfileRepository::class.java)
    private val authSessionRepository = mock(AuthSessionRepository::class.java)
    private val store = JpaDeletionRequestStore(
        repository = repository,
        profileStore = profileStore,
        profileRepository = profileRepository,
        authSessionRepository = authSessionRepository,
        clock = { Instant.EPOCH }
    )

    @Test
    fun `request saves deletion without revoking sessions when profile account is absent`() {
        val subject = "oidc|unprovisioned-${UUID.randomUUID()}"
        val entity = AccountDeletionRequestEntity(
            subject = subject,
            status = DeletionStatus.REQUESTED,
            requestedAt = Instant.EPOCH
        )
        `when`(repository.findById(subject)).thenReturn(Optional.empty())
        `when`(profileRepository.findBySubject(subject)).thenReturn(null)
        `when`(repository.save(any(AccountDeletionRequestEntity::class.java))).thenReturn(entity)

        val result = store.request(subject)

        assertEquals(subject, result.subject)
        assertEquals(DeletionStatus.REQUESTED, result.status)
        assertEquals(Instant.EPOCH, result.requestedAt)
        verify(profileStore).requestDeletion(subject)
        verifyNoInteractions(authSessionRepository)
    }
}
