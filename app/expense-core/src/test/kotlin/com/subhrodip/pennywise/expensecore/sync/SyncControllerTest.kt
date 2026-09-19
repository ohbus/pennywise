package com.subhrodip.pennywise.expensecore.sync

import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.security.Principal
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

import com.subhrodip.pennywise.ids.ApiEndpoints

class SyncControllerTest {
    private val store = SynchronizationStore(java.time.Clock.systemUTC())
    private val memberships: GroupMembershipRepository = mock(GroupMembershipRepository::class.java)
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(SyncController(store, memberships)).setControllerAdvice(GlobalErrorHandler()).build()
    private val user = RequestPostProcessor { request -> request.userPrincipal = Principal { "alice" }; request }
    private val groupId = UUID.randomUUID()

    init {
        `when`(memberships.existsByGroupIdAndSubjectAndStatus(groupId, "alice", "ACTIVE")).thenReturn(true)
    }

    @Test
    fun `hides sync data from non-members`() {
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncSnapshot(groupId)).with(RequestPostProcessor { request ->
            request.userPrincipal = Principal { "bob" }
            request
        }))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `returns ordered changes and cursor`() {
        store.append(groupId.toString(), "expense-1", "{}")
        store.delete(groupId.toString(), "expense-2")
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncChanges(groupId)).with(user).param("limit", "1"))
            .andExpect(status().isOk).andExpect(jsonPath("$.changes[0].revision").value(1)).andExpect(jsonPath("$.hasMore").value(true))
    }

    @Test
    fun `rejects invalid cursor and limit`() {
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncSnapshot(groupId)).with(user).param("cursor", "bad"))
            .andExpect(status().isBadRequest)
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncSnapshot(groupId)).with(user).param("limit", "101"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects a cursor created for another group`() {
        val otherGroup = UUID.randomUUID()
        store.append(otherGroup.toString(), "expense-1", "{}")
        val cursor = store.snapshot(otherGroup.toString(), null, 1).nextCursor!!
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncChanges(groupId)).with(user).param("cursor", cursor))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects an expired cursor through both public sync routes`() {
        val otherStore = SynchronizationStore(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC))
        otherStore.append(groupId.toString(), "expense-1", "{}")
        val cursor = otherStore.snapshot(groupId.toString(), null, 1).nextCursor!!
        val expiredStore = SynchronizationStore(java.time.Clock.fixed(java.time.Instant.parse("2026-01-02T00:00:00Z"), java.time.ZoneOffset.UTC))
        val expiredMvc = MockMvcBuilders.standaloneSetup(SyncController(expiredStore, memberships)).setControllerAdvice(GlobalErrorHandler()).build()

        expiredMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncSnapshot(groupId)).with(user).param("cursor", cursor))
            .andExpect(status().isBadRequest)
        expiredMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSyncChanges(groupId)).with(user).param("cursor", cursor))
            .andExpect(status().isBadRequest)
    }
}
