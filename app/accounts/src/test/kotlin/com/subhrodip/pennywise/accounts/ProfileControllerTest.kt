package com.subhrodip.pennywise.accounts

import com.subhrodip.pennywise.accounts.requests.deletion.DeletionRequestService
import com.subhrodip.pennywise.accounts.requests.export.ExportRequestService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.ids.ApiEndpoints
import java.nio.charset.StandardCharsets
import java.security.Principal
import java.util.UUID
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProfileControllerTest {
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(
        ProfileController(
            profiles = InMemoryProfileStore(),
            deletionService = DeletionRequestService(),
            exportService = ExportRequestService()
        )
    )
        .setControllerAdvice(GlobalErrorHandler())
        .build()
    private val alice = RequestPostProcessor { request ->
        request.userPrincipal = Principal { "oidc|alice" }
        request
    }

    @Test
    fun `gets profile for authenticated subject`() {
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("oidc|alice"))
            .andExpect(jsonPath("$.defaultCurrency").value("EUR"))
    }

    @Test
    fun `updates profile with validated fields`() {
        mvc.perform(patch(ApiEndpoints.Accounts.V1.PATH_ME).with(alice).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Alice\",\"timezone\":\"Europe/Vienna\",\"defaultCurrency\":\"USD\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("Alice"))
            .andExpect(jsonPath("$.timezone").value("Europe/Vienna"))
            .andExpect(jsonPath("$.defaultCurrency").value("USD"))
    }

    @Test
    fun `rejects empty patch`() {
        mvc.perform(patch(ApiEndpoints.Accounts.V1.PATH_ME).with(alice).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `accepts deletion request`() {
        assertEquals(202, mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_ME_DELETION_REQUEST).with(alice)).andReturn().response.status)
    }

    @Test
    fun `creates export request for authenticated subject`() {
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUEST).with(alice))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.exportId").isNotEmpty)
            .andExpect(jsonPath("$.status").value("REQUESTED"))
            .andExpect(jsonPath("$.requestedAt").isNotEmpty)
    }

    @Test
    fun `lists export requests for authenticated subject`() {
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUEST).with(alice))
            .andExpect(status().isAccepted)

        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUESTS).with(alice))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].exportId").isNotEmpty)
            .andExpect(jsonPath("$[0].status").value("REQUESTED"))
            .andExpect(jsonPath("$[0].requestedAt").isNotEmpty)
    }

    @Test
    fun `rejects export request without authentication`() {
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUEST))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects export request listing without authentication`() {
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUESTS))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects export request with invalid subject`() {
        val invalidUser = RequestPostProcessor { request ->
            request.userPrincipal = Principal { "invalid subject with spaces" }
            request
        }
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUEST).with(invalidUser))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects export request listing with invalid subject`() {
        val invalidUser = RequestPostProcessor { request ->
            request.userPrincipal = Principal { "invalid subject with spaces" }
            request
        }
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME_EXPORT_REQUESTS).with(invalidUser))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `gets profile by account id`() {
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME).with(alice))
            .andExpect(status().isOk)

        val accountId = UUID.nameUUIDFromBytes("oidc|alice".toByteArray(StandardCharsets.UTF_8))
        mvc.perform(get(ApiEndpoints.Accounts.V1.profileById(accountId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(accountId.toString()))
            .andExpect(jsonPath("$.displayName").value("oidc|alice"))
            .andExpect(jsonPath("$.timezone").value("UTC"))
            .andExpect(jsonPath("$.defaultCurrency").value("EUR"))
    }

    @Test
    fun `returns 404 when profile not found by account id`() {
        val nonExistentId = UUID.randomUUID()
        mvc.perform(get(ApiEndpoints.Accounts.V1.profileById(nonExistentId)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }

    @Test
    fun `store finds profile by account id and returns null when absent`() {
        val store = InMemoryProfileStore()
        val created = store.get("oidc|bob")
        assertEquals(created, store.findById(created.accountId))
        org.junit.jupiter.api.Assertions.assertNull(store.findById(UUID.randomUUID()))
    }

    @Test
    fun `gets profiles in batch for valid account ids`() {
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME).with(alice))
            .andExpect(status().isOk)

        val bob = RequestPostProcessor { request ->
            request.userPrincipal = Principal { "oidc|bob" }
            request
        }
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME).with(bob))
            .andExpect(status().isOk)

        val aliceId = UUID.nameUUIDFromBytes("oidc|alice".toByteArray(StandardCharsets.UTF_8))
        val bobId = UUID.nameUUIDFromBytes("oidc|bob".toByteArray(StandardCharsets.UTF_8))
        val nonExistentId = UUID.randomUUID()

        val requestBody = "{\"accountIds\": [\"$aliceId\", \"$bobId\", \"$nonExistentId\"]}"
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))

        val emptyBatch = "{\"accountIds\": [\"$nonExistentId\"]}"
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyBatch))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `rejects batch profile lookup with empty account ids`() {
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountIds\": []}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `rejects batch profile lookup with invalid uuid`() {
        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountIds\": [\"not-a-valid-uuid\"]}"))
            .andExpect(status().isBadRequest)
    }

    /** Verifies the public batch boundary enforces its documented maximum of 100 account IDs. */
    @Test
    fun `rejects batch profile lookup above maximum size`() {
        val accountIds = (1..101).joinToString(",") { "\"${UUID.randomUUID()}\"" }

        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountIds\":[$accountIds]}"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    /** Verifies duplicate requested IDs produce one profile rather than duplicated response rows. */
    @Test
    fun `deduplicates repeated profile identifiers in batch response`() {
        mvc.perform(get(ApiEndpoints.Accounts.V1.PATH_ME).with(alice))
            .andExpect(status().isOk)
        val aliceId = UUID.nameUUIDFromBytes("oidc|alice".toByteArray(StandardCharsets.UTF_8))

        mvc.perform(post(ApiEndpoints.Accounts.V1.PATH_PROFILES_BATCH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountIds\":[\"$aliceId\",\"$aliceId\"]}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].accountId").value(aliceId.toString()))
    }

    @Test
    fun `store finds profiles in batch and ignores non-existent ids`() {
        val store = InMemoryProfileStore()
        val p1 = store.get("oidc|user-1")
        val p2 = store.get("oidc|user-2")
        val p3 = store.get("oidc|user-3")

        val result = store.findByIds(listOf(p1.accountId, p3.accountId, UUID.randomUUID()))
        assertEquals(2, result.size)
        val resultIds = result.map { it.accountId }.toSet()
        assertTrue(resultIds.contains(p1.accountId))
        assertTrue(resultIds.contains(p3.accountId))
    }
}
