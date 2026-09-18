package com.subhrodip.pennywise.expensecore.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
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
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.ids.ApiEndpoints

/**
 * Tests for [GroupController] and [InviteClaimController] REST endpoints, covering group lifecycle,
 * invitations, memberships, and group-rename validation and authorization edge cases.
 */
class GroupControllerTest {
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(GroupController(InMemoryGroupStore()))
        .setControllerAdvice(GlobalErrorHandler()).build()
    private val alice = RequestPostProcessor { request -> request.userPrincipal = Principal { "alice" }; request }

    /**
     * Verifies that a valid group creation request establishes the group with initial revision 0
     * and makes it discoverable when listed by the creating subject.
     */
    @Test
    fun `creates and lists a group for subject`() {
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Vienna trip\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andExpect(jsonPath("$.name").value("Vienna trip"))
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

        val bob = RequestPostProcessor { request -> request.userPrincipal = Principal { "bob" }; request }
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

        val bob = RequestPostProcessor { request -> request.userPrincipal = Principal { "bob" }; request }
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

        val bob = RequestPostProcessor { request -> request.userPrincipal = Principal { "bob" }; request }
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
     * Verifies that renaming a group with leading or trailing whitespace trims the whitespace
     * before persisting and returning the updated group.
     */
    @Test
    fun `renames group and trims leading and trailing whitespace`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Original\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   Trimmed Name   \"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Trimmed Name"))
            .andExpect(jsonPath("$.revision").value(1))
    }

    /**
     * Verifies that renaming a group with Unicode / multi-byte characters succeeds and preserves the exact Unicode string.
     */
    @Test
    fun `renames group with unicode name`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Original\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val unicodeName = "Wien Ausflug 🏔️ 🍕 \uD83C\uDDE6\uD83C\uDDF9 日本語"
        val requestJson = patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId))
            .with(alice)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"$unicodeName\"}")

        mvc.perform(requestJson)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(unicodeName))
            .andExpect(jsonPath("$.revision").value(1))
    }

    /**
     * Verifies that repeated identical rename requests succeed and increment the revision counter on each invocation.
     */
    @Test
    fun `repeated identical rename requests increment revision`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Initial Name\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Identical Name\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Identical Name"))
            .andExpect(jsonPath("$.revision").value(1))

        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Identical Name\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Identical Name"))
            .andExpect(jsonPath("$.revision").value(2))
    }

    /**
     * Verifies that renaming with boundary length 120 characters succeeds, while length 121 is rejected
     * with 400 Bad Request and GlobalErrorHandler ProblemDetail code VALIDATION_FAILED.
     * Also verifies that the rejected request does not mutate group name or increment revision.
     */
    @Test
    fun `renames group at boundary length 120 succeeds and 121 fails validation`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Initial\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val name120 = "A".repeat(120)
        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"$name120\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(name120))
            .andExpect(jsonPath("$.revision").value(1))

        val name121 = "B".repeat(121)
        val rejectedResult = mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"$name121\"}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andReturn()

        assertEquals("VALIDATION_FAILED", Regex("\\\"code\\\":\\\"([^\\\"]+)\\\"").find(rejectedResult.response.contentAsString)!!.groupValues[1])

        // Verify group unchanged
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(name120))
            .andExpect(jsonPath("$.revision").value(1))
    }

    /**
     * Verifies that blank and whitespace-only names are rejected through the REST boundary with
     * 400 Bad Request and GlobalErrorHandler ProblemDetail code VALIDATION_FAILED, and do not increment revision.
     */
    @Test
    fun `rejects blank and whitespace only names with validation failed code`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Stable Group\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val blankPayloads = listOf("{\"name\":\"\"}", "{\"name\":\"   \"}", "{\"name\":\"\\t\\n  \"}")
        for (payload in blankPayloads) {
            mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations").isArray)
        }

        // Verify group unchanged
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Stable Group"))
            .andExpect(jsonPath("$.revision").value(0))
    }

    /**
     * Verifies that malformed JSON payloads return 400 Bad Request with ProblemDetail code VALIDATION_FAILED,
     * and do not increment group revision.
     */
    @Test
    fun `rejects malformed json payload with validation failed code`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Initial Name\",\"kind\":\"TRIP\",\"currency\":\"EUR\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{ malformed json }"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))

        // Verify group unchanged
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Initial Name"))
            .andExpect(jsonPath("$.revision").value(0))
    }

    /**
     * Verifies that attempting to rename a non-existent group ID returns 404 Not Found with code NOT_FOUND.
     */
    @Test
    fun `returns 404 when group does not exist`() {
        val nonExistentGroupId = java.util.UUID.randomUUID()
        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(nonExistentGroupId)).with(alice)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New Name\"}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    /**
     * Verifies that attempting to rename a group by an authenticated non-member returns 404 Not Found
     * (preventing group enumeration/discovery), and does not modify the group.
     */
    @Test
    fun `returns 404 when non-member attempts group rename`() {
        val created = mvc.perform(post(ApiEndpoints.ExpenseCore.V1.PATH_GROUPS).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Private Group\",\"kind\":\"HOUSEHOLD\",\"currency\":\"USD\"}"))
            .andExpect(status().isCreated).andReturn().response.contentAsString
        val groupId = Regex("\\\"groupId\\\":\\\"([^\\\"]+)\\\"").find(created)!!.groupValues[1]

        val bob = RequestPostProcessor { request -> request.userPrincipal = Principal { "bob" }; request }
        mvc.perform(patch(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(bob)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijacked\"}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))

        // Verify group unchanged
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupById(groupId)).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Private Group"))
            .andExpect(jsonPath("$.revision").value(0))
    }

    /**
     * Verifies [InMemoryGroupStore] direct update behavior: trims name, increments revision,
     * and rejects non-member subjects with 404 ResponseStatusException.
     */
    @Test
    fun `in-memory store update trims name and increments revision`() {
        val store = InMemoryGroupStore()
        val group = store.create("alice", CreateGroupRequest("Original", "TRIP", "EUR"))
        val updated = store.update(group.groupId, "alice", UpdateGroupRequest("  Trimmed  "))
        assertEquals("Trimmed", updated.name)
        assertEquals(1, updated.revision)

        org.junit.jupiter.api.assertThrows<org.springframework.web.server.ResponseStatusException> {
            store.update(group.groupId, "intruder", UpdateGroupRequest("Bad"))
        }
    }
}
