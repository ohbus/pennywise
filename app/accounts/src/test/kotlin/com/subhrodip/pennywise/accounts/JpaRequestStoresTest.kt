package com.subhrodip.pennywise.accounts

import com.subhrodip.pennywise.accounts.requests.deletion.persistence.DeletionRequestRepository
import com.subhrodip.pennywise.accounts.requests.deletion.persistence.JpaDeletionRequestStore
import com.subhrodip.pennywise.accounts.requests.deletion.model.DeletionStatus
import com.subhrodip.pennywise.accounts.requests.export.persistence.ExportRequestRepository
import com.subhrodip.pennywise.accounts.requests.export.model.ExportStatus
import com.subhrodip.pennywise.accounts.requests.export.persistence.JpaExportRequestStore
import com.subhrodip.pennywise.accounts.profile.persistence.JpaProfileStore
import com.subhrodip.pennywise.accounts.profile.persistence.ProfileRepository

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class JpaRequestStoresTest @Autowired constructor(
    private val deletionStore: JpaDeletionRequestStore,
    private val exportStore: JpaExportRequestStore,
    private val profileStore: JpaProfileStore,
    private val profileRepository: ProfileRepository,
    private val deletionRepository: DeletionRequestRepository,
    private val exportRepository: ExportRequestRepository
) {

    @Test
    fun `persists deletion request idempotently and preserves historical financial profile reference`() {
        val subject = "oidc|user-${UUID.randomUUID()}"

        // Initial profile creation
        val profile = profileStore.get(subject)
        assertEquals(subject, profile.displayName)

        // Request deletion
        val req1 = deletionStore.request(subject)
        assertEquals(subject, req1.subject)
        assertEquals(DeletionStatus.REQUESTED, req1.status)

        // Verify entity in database
        val entity = deletionRepository.findById(subject).orElse(null)
        assertNotNull(entity)
        assertEquals(DeletionStatus.REQUESTED, entity?.status)

        // Verify profile record is preserved for financial attribution but marked deletion_requested = true
        val storedProfile = profileRepository.findBySubject(subject)
        assertNotNull(storedProfile)
        assertEquals(profile.accountId, storedProfile?.accountId)
        assertTrue(storedProfile?.deletionRequested == true)

        // Idempotency: duplicate request returns existing record without duplicating or throwing
        val req2 = deletionStore.request(subject)
        assertEquals(req1.subject, req2.subject)
        assertEquals(req1.requestedAt, req2.requestedAt)
        assertEquals(req1.status, req2.status)
        assertEquals(1, deletionRepository.findAll().count { it.subject == subject })

        // Lifecycle transitions: cancel
        val cancelled = deletionStore.cancel(subject)
        assertEquals(DeletionStatus.CANCELLED, cancelled?.status)
        assertEquals(DeletionStatus.CANCELLED, deletionRepository.findById(subject).get().status)

        // Lifecycle transitions: complete
        val completed = deletionStore.complete(subject)
        assertEquals(DeletionStatus.COMPLETED, completed?.status)
        assertEquals(DeletionStatus.COMPLETED, deletionRepository.findById(subject).get().status)
    }

    @Test
    fun `persists and queries export requests by id and subject`() {
        val subject = "oidc|user-${UUID.randomUUID()}"

        val exp1 = exportStore.request(subject)
        assertNotNull(exp1.exportId)
        assertEquals(subject, exp1.subject)
        assertEquals(ExportStatus.REQUESTED, exp1.status)

        val exp2 = exportStore.request(subject)
        assertNotNull(exp2.exportId)
        assertTrue(exp1.exportId != exp2.exportId)

        // Query by id
        val retrieved = exportStore.get(exp1.exportId)
        assertNotNull(retrieved)
        assertEquals(exp1.exportId, retrieved?.exportId)
        assertEquals(ExportStatus.REQUESTED, retrieved?.status)

        // Non-existent id
        assertNull(exportStore.get(UUID.randomUUID()))

        // List by subject
        val list = exportStore.listBySubject(subject)
        assertEquals(2, list.size)
        assertEquals(listOf(exp2.exportId, exp1.exportId), list.map { it.exportId })
    }

    @Test
    fun `finds profile by account id`() {
        val subject = "oidc|user-${UUID.randomUUID()}"
        val profile = profileStore.get(subject)

        val found = profileStore.findById(profile.accountId)
        assertNotNull(found)
        assertEquals(profile.accountId, found?.accountId)
        assertEquals(profile.displayName, found?.displayName)
        assertEquals(profile.timezone, found?.timezone)
        assertEquals(profile.defaultCurrency, found?.defaultCurrency)

        val notFound = profileStore.findById(UUID.randomUUID())
        assertNull(notFound)
    }

    @Test
    fun `finds profiles in batch by account ids`() {
        val subject1 = "oidc|user-${UUID.randomUUID()}"
        val subject2 = "oidc|user-${UUID.randomUUID()}"
        val p1 = profileStore.get(subject1)
        val p2 = profileStore.get(subject2)

        val batch = profileStore.findByIds(listOf(p1.accountId, p2.accountId, UUID.randomUUID()))
        assertEquals(2, batch.size)
        val ids = batch.map { it.accountId }.toSet()
        assertTrue(ids.contains(p1.accountId))
        assertTrue(ids.contains(p2.accountId))
    }
}
