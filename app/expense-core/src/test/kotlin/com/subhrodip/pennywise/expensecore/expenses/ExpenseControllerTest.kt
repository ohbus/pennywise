package com.subhrodip.pennywise.expensecore.expenses

import com.subhrodip.pennywise.errors.GlobalErrorHandler
import java.util.UUID
import java.security.Principal
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipRepository
import com.subhrodip.pennywise.expensecore.groups.GroupRepository
import com.subhrodip.pennywise.expensecore.groups.GroupEntity
import java.util.Optional
import java.lang.reflect.Proxy
import org.mockito.Mockito.`when`
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import com.subhrodip.pennywise.ids.ApiEndpoints

class ExpenseControllerTest {
    private val store = InMemoryExpenseStore()
    private val archivedGroups = mutableSetOf<UUID>()
    private val memberships = Proxy.newProxyInstance(
        GroupMembershipRepository::class.java.classLoader,
        arrayOf(GroupMembershipRepository::class.java)
    ) { _, method, args ->
        if (method.name.startsWith("existsByGroupIdAndSubject")) args?.getOrNull(1) == "test-user" else null
    } as GroupMembershipRepository
    private val groups = Proxy.newProxyInstance(
        GroupRepository::class.java.classLoader,
        arrayOf(GroupRepository::class.java)
    ) { _, method, args ->
        if (method.name == "findById") {
            val groupId = args!![0] as UUID
            Optional.of(GroupEntity(groupId, "Test", "TRIP", "EUR", if (groupId in archivedGroups) "ARCHIVED" else "ACTIVE"))
        } else null
    } as GroupRepository
    private val mvcBuilder = MockMvcBuilders.standaloneSetup(ExpenseController(store, memberships, groups))
        .setControllerAdvice(GlobalErrorHandler())
        .setCustomArgumentResolvers(object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean =
                parameter.parameterType == Principal::class.java

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?
            ): Any = webRequest.userPrincipal ?: Principal { "test-user" }
        })

    private val mvc: MockMvc = mvcBuilder.build()

    @Test
    fun `rejects expense creation for non-member`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val participantId = UUID.randomUUID()
        val json = """
            {
                "expenseId": "$expenseId", "description": "Unauthorized", "amount": {"currency": "EUR", "minor": "100"},
                "payers": [{"participantId": "$participantId", "amount": {"currency": "EUR", "minor": "100"}}],
                "allocation": {"mode": "EQUAL", "items": [{"participantId": "$participantId", "value": "1"}]}
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .principal(Principal { "non-member" })
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "non-member-expense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andExpect(status().isNotFound)

        mvc.perform(
            get(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .principal(Principal { "non-member" })
        ).andExpect(status().isNotFound)

        mvc.perform(
            get(ApiEndpoints.ExpenseCore.V1.groupBalances(groupId))
                .principal(Principal { "non-member" })
        ).andExpect(status().isNotFound)

        mvc.perform(
            put(ApiEndpoints.ExpenseCore.V1.groupExpenseById(groupId, expenseId))
                .principal(Principal { "non-member" })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "version": 1, "description": "Unauthorized", "amount": {"currency": "EUR", "minor": "100"},
                        "payers": [{"participantId": "$participantId", "amount": {"currency": "EUR", "minor": "100"}}],
                        "allocation": {"mode": "EQUAL", "items": [{"participantId": "$participantId", "value": "1"}]}
                    }
                """.trimIndent())
        ).andExpect(status().isNotFound)

        mvc.perform(
            delete(ApiEndpoints.ExpenseCore.V1.groupExpenseById(groupId, expenseId) + "?version=1")
                .principal(Principal { "non-member" })
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `rejects expense creation for archived group`() {
        val groupId = UUID.randomUUID()
        archivedGroups += groupId
        val participantId = UUID.randomUUID()
        val json = """
            {
                "expenseId": "${UUID.randomUUID()}", "description": "Archived", "amount": {"currency": "EUR", "minor": "100"},
                "payers": [{"participantId": "$participantId", "amount": {"currency": "EUR", "minor": "100"}}],
                "allocation": {"mode": "EQUAL", "items": [{"participantId": "$participantId", "value": "1"}]}
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "archived-group-expense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `creates expense with equal allocation successfully`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()

        val json = """
            {
                "expenseId": "$expenseId",
                "description": "Dinner in Vienna",
                "category": "food",
                "amount": { "currency": "EUR", "minor": "3000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "3000" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" },
                        { "participantId": "$bobId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-key-test-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.expenseId").value(expenseId.toString()))
            .andExpect(jsonPath("$.amount.minor").value("3000"))
            .andExpect(jsonPath("$.category").value("food"))
            .andExpect(jsonPath("$.allocations.length()").value(2))

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].expenseId").value(expenseId.toString()))

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupBalances(groupId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balances.length()").value(2))
    }

    @Test
    fun `creates expense with exact and percentage allocation modes`() {
        val groupId = UUID.randomUUID()
        val expenseId1 = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()

        val exactJson = """
            {
                "expenseId": "$expenseId1",
                "description": "Groceries",
                "category": "shopping",
                "amount": { "currency": "EUR", "minor": "1000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "1000" } }
                ],
                "allocation": {
                    "mode": "EXACT",
                    "items": [
                        { "participantId": "$aliceId", "value": "600" },
                        { "participantId": "$bobId", "value": "400" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-key-test-exact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exactJson)
        ).andExpect(status().isCreated)

        val expenseId2 = UUID.randomUUID()
        val percentJson = """
            {
                "expenseId": "$expenseId2",
                "description": "Taxi ride",
                "category": "transport",
                "amount": { "currency": "EUR", "minor": "2000" },
                "payers": [
                    { "participantId": "$bobId", "amount": { "currency": "EUR", "minor": "2000" } }
                ],
                "allocation": {
                    "mode": "PERCENT_BASIS_POINTS",
                    "items": [
                        { "participantId": "$aliceId", "value": "5000" },
                        { "participantId": "$bobId", "value": "5000" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-key-test-percent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(percentJson)
        ).andExpect(status().isCreated)
    }

    @Test
    fun `rejects expense when payer amounts do not sum to total`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val json = """
            {
                "expenseId": "$expenseId",
                "description": "Coffee",
                "amount": { "currency": "EUR", "minor": "1000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "800" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-key-test-mismatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `rejects request missing Idempotency-Key header`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val json = """
            {
                "expenseId": "$expenseId",
                "description": "Coffee",
                "amount": { "currency": "EUR", "minor": "500" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "500" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `updates expense successfully and increments version`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()

        val createJson = """
            {
                "expenseId": "$expenseId",
                "description": "Original lunch",
                "category": "food",
                "amount": { "currency": "EUR", "minor": "2000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "2000" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" },
                        { "participantId": "$bobId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-create-before-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        ).andExpect(status().isCreated)

        val updateJson = """
            {
                "version": 1,
                "description": "Updated lunch with desserts",
                "category": "food",
                "amount": { "currency": "EUR", "minor": "4000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "4000" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" },
                        { "participantId": "$bobId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            put(ApiEndpoints.ExpenseCore.V1.groupExpenseById(groupId, expenseId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expenseId").value(expenseId.toString()))
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.amount.minor").value("4000"))
    }

    @Test
    fun `rejects update when version is stale with 409 Conflict`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val createJson = """
            {
                "expenseId": "$expenseId",
                "description": "Coffee",
                "category": "food",
                "amount": { "currency": "EUR", "minor": "500" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "500" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-conflict-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        ).andExpect(status().isCreated)

        // Version mismatch: sending version 99 when version is 1
        val updateJson = """
            {
                "version": 99,
                "description": "Stale update",
                "category": "food",
                "amount": { "currency": "EUR", "minor": "500" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "500" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            put(ApiEndpoints.ExpenseCore.V1.groupExpenseById(groupId, expenseId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `deletes expense successfully and removes it from listing`() {
        val groupId = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        val createJson = """
            {
                "expenseId": "$expenseId",
                "description": "To be deleted",
                "category": "other",
                "amount": { "currency": "EUR", "minor": "1000" },
                "payers": [
                    { "participantId": "$aliceId", "amount": { "currency": "EUR", "minor": "1000" } }
                ],
                "allocation": {
                    "mode": "EQUAL",
                    "items": [
                        { "participantId": "$aliceId", "value": "1" }
                    ]
                }
            }
        """.trimIndent()

        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId))
                .header(ApiEndpoints.Headers.IDEMPOTENCY_KEY, "idemp-delete-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        ).andExpect(status().isCreated)

        mvc.perform(delete(ApiEndpoints.ExpenseCore.V1.groupExpenseById(groupId, expenseId)))
            .andExpect(status().isNoContent)

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    /** Verifies expense collection limits reject unbounded or empty pages at the public boundary. */
    @Test
    fun `rejects invalid expense list limits`() {
        val groupId = UUID.randomUUID()

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId)).param("limit", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupExpenses(groupId)).param("limit", "101"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }
}
