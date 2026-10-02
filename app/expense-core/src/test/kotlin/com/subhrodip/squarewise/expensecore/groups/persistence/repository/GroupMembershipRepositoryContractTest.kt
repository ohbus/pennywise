package com.subhrodip.squarewise.expensecore.groups.persistence.repository

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/** Verifies repository convenience methods consistently constrain queries to active members. */
class GroupMembershipRepositoryContractTest {
    @Test
    fun `active membership convenience methods delegate with active status`() {
        val repository = mock(GroupMembershipRepository::class.java, CALLS_REAL_METHODS)
        val groupId = UUID.randomUUID()
        val subject = "oidc|alice"

        `when`(repository.existsByGroupIdAndSubjectAndStatus(groupId, subject, "ACTIVE"))
            .thenReturn(true)
        `when`(repository.findAllBySubjectAndStatusOrderByMembershipId(subject, "ACTIVE"))
            .thenReturn(emptyList())
        `when`(repository.findByGroupIdAndStatus(groupId, "ACTIVE"))
            .thenReturn(emptyList())

        assertTrue(repository.existsByGroupIdAndSubject(groupId, subject))
        assertEquals(emptyList<Any>(), repository.findAllBySubjectOrderByMembershipId(subject))
        assertEquals(emptyList<Any>(), repository.findByGroupId(groupId))

        verify(repository).existsByGroupIdAndSubjectAndStatus(groupId, subject, "ACTIVE")
        verify(repository).findAllBySubjectAndStatusOrderByMembershipId(subject, "ACTIVE")
        verify(repository).findByGroupIdAndStatus(groupId, "ACTIVE")
    }
}
