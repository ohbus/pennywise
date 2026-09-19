package com.subhrodip.pennywise.expensecore.settlements

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID
import com.subhrodip.pennywise.errors.GlobalErrorHandler
import com.subhrodip.pennywise.expensecore.expenses.ExpenseAllocation
import com.subhrodip.pennywise.expensecore.expenses.ExpensePayer
import com.subhrodip.pennywise.expensecore.expenses.ExpenseRecord
import com.subhrodip.pennywise.expensecore.expenses.InMemoryExpenseStore
import java.time.Instant
import java.security.Principal
import java.lang.reflect.Proxy
import org.springframework.test.web.servlet.request.RequestPostProcessor
import com.subhrodip.pennywise.expensecore.groups.GroupMembershipRepository

import com.subhrodip.pennywise.ids.ApiEndpoints

class SettlementControllerTest {
    private val expenseStore = InMemoryExpenseStore()
    private val suggestionEngine = SettlementSuggestionEngine(expenseStore)
    private val service = SettlementService(InMemorySettlementStore(), suggestionEngine)
    private val memberships = Proxy.newProxyInstance(
        GroupMembershipRepository::class.java.classLoader,
        arrayOf(GroupMembershipRepository::class.java)
    ) { _, method, args ->
        if (method.name.startsWith("existsByGroupIdAndSubject")) args?.getOrNull(1) == "test-user" else null
    } as GroupMembershipRepository
    private val controller = SettlementController(service, memberships, suggestionEngine)
    private val mvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(GlobalErrorHandler()).build()
    private val user = RequestPostProcessor { request -> request.userPrincipal = Principal { "test-user" }; request }

    @Test
    fun `rejects non numeric amount`() {
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupSettlements(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromParticipantId\":\"${UUID.randomUUID()}\",\"toParticipantId\":\"${UUID.randomUUID()}\",\"amountMinor\":\"x\"}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns settlement suggestions successfully`() {
        val groupId = UUID.randomUUID()
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()

        val record = ExpenseRecord(
            expenseId = UUID.randomUUID(),
            groupId = groupId,
            description = "Lunch",
            category = "food",
            currency = "USD",
            amountMinor = 1000,
            version = 1,
            allocationMode = "EQUAL",
            createdAt = Instant.now(),
            payers = listOf(ExpensePayer(alice, 1000)),
            allocations = listOf(ExpenseAllocation(alice, 500), ExpenseAllocation(bob, 500))
        )
        expenseStore.create(groupId, record, "test-key-1")

        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSettlementSuggestions(groupId)).with(user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fromParticipantId").value(bob.toString()))
            .andExpect(jsonPath("$[0].toParticipantId").value(alice.toString()))
            .andExpect(jsonPath("$[0].amountMinor").value(500))
            .andExpect(jsonPath("$[0].currency").value("USD"))
    }

    @Test
    fun `returns empty list when no debts exist`() {
        val groupId = UUID.randomUUID()
        mvc.perform(get(ApiEndpoints.ExpenseCore.V1.groupSettlementSuggestions(groupId)).with(user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `hides settlement suggestions from non-members`() {
        val nonMember = RequestPostProcessor { request -> request.userPrincipal = Principal { "non-member" }; request }

        mvc.perform(
            get(ApiEndpoints.ExpenseCore.V1.groupSettlementSuggestions(UUID.randomUUID())).with(nonMember)
        ).andExpect(status().isNotFound)
    }

    /** Verifies recording, reversal, and reversal replay through the public REST boundary. */
    @Test
    fun `records and idempotently reverses settlement`() {
        val groupId = UUID.randomUUID()
        val from = UUID.randomUUID()
        val to = UUID.randomUUID()
        val recorded = mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.groupSettlements(groupId)).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromParticipantId\":\"$from\",\"toParticipantId\":\"$to\",\"amountMinor\":\"1250\"}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fromParticipantId").value(from.toString()))
            .andExpect(jsonPath("$.toParticipantId").value(to.toString()))
            .andExpect(jsonPath("$.amountMinor").value(1250))
            .andExpect(jsonPath("$.status").value("RECORDED"))
            .andReturn().response.contentAsString
        val settlementId = Regex("\\\"id\\\":\\\"([^\\\"]+)\\\"").find(recorded)!!.groupValues[1]
        val reversalPath = ApiEndpoints.ExpenseCore.V1.groupSettlementReversal(groupId, settlementId)

        mvc.perform(post(reversalPath).with(user).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"paid externally\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REVERSED"))
            .andExpect(jsonPath("$.reason").value("paid externally"))

        mvc.perform(post(reversalPath).with(user).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"retry must not overwrite\"}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REVERSED"))
            .andExpect(jsonPath("$.reason").value("paid externally"))
    }

    /** Verifies invalid settlement invariants and unknown reversals use public client errors. */
    @Test
    fun `rejects invalid settlements and returns not found for unknown reversal`() {
        val groupId = UUID.randomUUID()
        val participant = UUID.randomUUID()
        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupSettlements(groupId)).with(user)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromParticipantId\":\"$participant\",\"toParticipantId\":\"$participant\",\"amountMinor\":\"100\"}"))
            .andExpect(status().isBadRequest)

        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupSettlements(groupId)).with(user)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromParticipantId\":\"${UUID.randomUUID()}\",\"toParticipantId\":\"${UUID.randomUUID()}\",\"amountMinor\":\"0\"}"))
            .andExpect(status().isBadRequest)

        mvc.perform(post(ApiEndpoints.ExpenseCore.V1.groupSettlementReversal(groupId, UUID.randomUUID())).with(user)
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"unknown\"}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    }
}
