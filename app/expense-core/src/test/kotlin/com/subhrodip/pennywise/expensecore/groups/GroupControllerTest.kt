package com.subhrodip.pennywise.expensecore.groups
import com.subhrodip.pennywise.expensecore.groups.api.CreateGroupRequest
import com.subhrodip.pennywise.expensecore.groups.api.CreateInviteRequest
import com.subhrodip.pennywise.expensecore.groups.api.GroupController
import com.subhrodip.pennywise.expensecore.groups.api.InviteClaimController
import com.subhrodip.pennywise.expensecore.groups.persistence.store.InMemoryGroupStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.security.Principal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import com.subhrodip.pennywise.errors.http.GlobalErrorHandler
import com.subhrodip.pennywise.ids.contracts.ApiEndpoints

/**
 * Tests for [GroupController] and [InviteClaimController] REST endpoints, covering group lifecycle,
 * archiving, placeholders, member removal, invitation revocation, and authorization edge cases.
 */
class GroupControllerTest {
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(GroupController(InMemoryGroupStore()))
        .setControllerAdvice(GlobalErrorHandler()).build()
    private val alice = RequestPostProcessor { request -> request.userPrincipal = Principal { "alice" }; request }
    private val bob = RequestPostProcessor { request -> request.userPrincipal = Principal { "bob" }; request }

    /**
     * Verifies that a valid group creation request establishes the group with initial revision 0
     * and makes it discoverable when listed by the creating subject.
     */
    @Test
    fun `creates and lists a group for subject`() {
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Vienna trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andExpect(jsonPath("$.name").value("Vienna trip"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice))
            .andExpect(status().isOk).andExpect(jsonPath("$[0].revision").value(0))
    }

    /**
     * Verifies that invalid group creation payloads return 400 Bad Request with ProblemDetail code VALIDATION_FAILED.
     */
    @Test
    fun `rejects invalid group request`() {
        val result = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\",\"kind\":\"INVALID\",\"currency\":\"eur\"}"))
            .andExpect(status().isBadRequest).andReturn()
        assertEquals("VALIDATION_FAILED", result.response.getContentAsString().let { body ->
            Regex("\\\"code\\\":\\\"([^\\\"]+)\\\"").find(body)!!.groupValues[1]
        })
    }

    /**
     * Verifies that group invitations can only be claimed once across concurrent attempts.
     */
    @Test
    fun `creates and claims invitation only once`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]
        val invite = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId)).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expiresInHours\":24}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)\\\"").find(invite)!!.groupValues[1]
        val store = InMemoryGroupStore()
        val group = store.create("alice", CreateGroupRequest("Trip", "TRIP", "EUR"))
        val createdInvite = store.invite(group.groupId, "alice", CreateInviteRequest(24))
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val results = (1..8).map { index -> executor.submit(Callable {
            start.await()
            runCatching { store.claim(createdInvite.token, "member-$index") }.isSuccess
        }) }
        start.countDown()
        val successfulClaims = results.count { it.get() }
        executor.shutdown()
        assertEquals(1, successfulClaims)
        assertEquals(1, (1..8).sumOf { store.list("member-$it").size })
    }

    /**
     * Verifies that fetching a group by ID succeeds for an authorized member and returns 404 for non-members or missing groups.
     */
    @Test
    fun `gets group by id for member and returns 404 for non-existent or non-member`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Berlin Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.groupId").value(groupId))
            .andExpect(jsonPath("$.name").value("Berlin Trip"))

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(bob))
            .andExpect(status().isNotFound)
    }

    /**
     * Verifies that group membership lists are visible to members and return 404 for non-members.
     */
    @Test
    fun `lists members for group member and returns 404 for non-member`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Rome Holiday\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].groupId").value(groupId))
            .andExpect(jsonPath("$[0].subject").value("alice"))
            .andExpect(jsonPath("$[0].membershipId").isNotEmpty)

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(bob))
            .andExpect(status().isNotFound)
    }

    /**
     * Verifies that all group members appear in the member list after claiming an invitation.
     */
    @Test
    fun `lists all group members after invite claimed`() {
        val store = InMemoryGroupStore()
        val controller = GroupController(store)
        val testMvc = MockMvcBuilders.standaloneSetup(controller, InviteClaimController(store))
            .setControllerAdvice(GlobalErrorHandler()).build()

        val created = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Paris Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val invite = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId)).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expiresInHours\":24}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)\\\"").find(invite)!!.groupValues[1]

        testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.inviteClaim(token)).with(bob))
            .andExpect(status().isOk)

        testMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(bob))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].subject").value("alice"))
            .andExpect(jsonPath("$[1].subject").value("bob"))
    }

    /**
     * Verifies that a member can rename a group via PATCH and that the revision counter increments by 1.
     */
    @Test
    fun `renames group via PATCH and increments revision`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Old Name\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New Name\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.revision").value(1))

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Name"))
    }

    /**
     * Verifies archiving a group transitions status to ARCHIVED and rejects subsequent write mutations with 409 CONFLICT.
     */
    @Test
    fun `archives group and rejects subsequent mutations`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Active Household\",\"kind\":\"HOUSEHOLD\",\"currency\":\"USD\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        // Archive group
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupById(groupId) + "/archive").with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.revision").value(1))

        // Subsequent archive returns 409 Conflict
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupById(groupId) + "/archive").with(alice))
            .andExpect(status().isConflict)

        // Rename returns 409 Conflict
        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Archived Name\"}"))
            .andExpect(status().isConflict)
    }

    /**
     * Verifies creating a named placeholder and claiming an invitation linked to it binds the user's subject.
     */
    @Test
    fun `creates placeholder and claims targeted invite`() {
        val store = InMemoryGroupStore()
        val controller = GroupController(store)
        val testMvc = MockMvcBuilders.standaloneSetup(controller, InviteClaimController(store))
            .setControllerAdvice(GlobalErrorHandler()).build()

        val created = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        // Add placeholder
        val placeholderRes = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupById(groupId) + "/placeholders").with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Charlie Placeholder\"}"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.displayName").value("Charlie Placeholder"))
            .andExpect(jsonPath("$.isPlaceholder").value(true))
            .andReturn().response.contentAsString
        val placeholderId = Regex("\\\"membershipId\\\":\\\"([^\\\"]+)\\\"").find(placeholderRes)!!.groupValues[1]

        // Create invite targeting placeholder
        val inviteRes = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"expiresInHours\":24, \"placeholderId\":\"$placeholderId\"}"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.placeholderId").value(placeholderId))
            .andReturn().response.contentAsString
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)\\\"").find(inviteRes)!!.groupValues[1]

        // Bob claims targeted invite
        testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.inviteClaim(token)).with(bob))
            .andExpect(status().isOk)

        // Check members list: placeholder converted to bound subject bob with same membershipId
        val membersRes = testMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andReturn().response.contentAsString

        assertTrue(membersRes.contains("\"membershipId\":\"$placeholderId\""))
        assertTrue(membersRes.contains("\"subject\":\"bob\""))
    }

    /**
     * Verifies soft-removing a member excludes them from active member list and blocks subsequent API access.
     */
    @Test
    fun `removes group member soft delete and blocks access`() {
        val store = InMemoryGroupStore()
        val controller = GroupController(store)
        val testMvc = MockMvcBuilders.standaloneSetup(controller, InviteClaimController(store))
            .setControllerAdvice(GlobalErrorHandler()).build()

        val created = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val invite = testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId)).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expiresInHours\":24}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)\\\"").find(invite)!!.groupValues[1]

        testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.inviteClaim(token)).with(bob))
            .andExpect(status().isOk)

        val membersRes = testMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(alice))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val bobMembershipId = store.listMembers(java.util.UUID.fromString(groupId), "alice").find { it.subject == "bob" }!!.membershipId

        // Remove Bob
        testMvc.perform(delete(ApiEndpoints.ExpenseCore.V1.groupById(groupId) + "/members/$bobMembershipId").with(alice))
            .andExpect(status().isNoContent)

        // Removing the same membership again is a conflict and does not mutate revision twice.
        testMvc.perform(delete(ApiEndpoints.ExpenseCore.V1.groupById(groupId) + "/members/$bobMembershipId").with(alice))
            .andExpect(status().isConflict)

        // Active members list now has 1 member (alice)
        testMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupMembers(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        // Bob can no longer access group details
        testMvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(bob))
            .andExpect(status().isNotFound)
    }

    /**
     * Verifies revoking an invitation prevents subsequent claim attempts.
     */
    @Test
    fun `revokes invitation token and prevents claiming`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val invite = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId)).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"expiresInHours\":24}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)\\\"").find(invite)!!.groupValues[1]

        // Revoke invite
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId) + "/$token/revoke").with(alice))
            .andExpect(status().isNoContent)

        // Revocation replay is rejected without changing the invite again.
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupInvites(groupId) + "/$token/revoke").with(alice))
            .andExpect(status().isConflict)

        // Attempting to claim revoked invite returns 409 Conflict
        val store = InMemoryGroupStore()
        val controller = GroupController(store)
        val testMvc = MockMvcBuilders.standaloneSetup(controller, InviteClaimController(store))
            .setControllerAdvice(GlobalErrorHandler()).build()

        testMvc.perform(post(ApiEndpoints.ExpenseCore.V1.inviteClaim(token)).with(bob))
            .andExpect(status().isConflict)
    }
}
