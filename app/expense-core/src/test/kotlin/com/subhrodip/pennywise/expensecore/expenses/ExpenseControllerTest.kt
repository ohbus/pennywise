package com.subhrodip.pennywise.expensecore.expenses

import com.subhrodip.pennywise.errors.GlobalErrorHandler
import java.util.UUID
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

class ExpenseControllerTest {
    private val store = InMemoryExpenseStore()
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(ExpenseController(store))
        .setControllerAdvice(GlobalErrorHandler()).build()

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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-key-test-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.expenseId").value(expenseId.toString()))
            .andExpect(jsonPath("$.amount.minor").value("3000"))
            .andExpect(jsonPath("$.category").value("food"))
            .andExpect(jsonPath("$.allocations.length()").value(2))

        mvc.perform(get("/expense-core/v1/groups/$groupId/expenses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].expenseId").value(expenseId.toString()))

        mvc.perform(get("/expense-core/v1/groups/$groupId/balances"))
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-key-test-exact")
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-key-test-percent")
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-key-test-mismatch")
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
            post("/expense-core/v1/groups/$groupId/expenses")
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-create-before-update")
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
            put("/expense-core/v1/groups/$groupId/expenses/$expenseId")
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-conflict-test")
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
            put("/expense-core/v1/groups/$groupId/expenses/$expenseId")
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
            post("/expense-core/v1/groups/$groupId/expenses")
                .header("Idempotency-Key", "idemp-delete-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson)
        ).andExpect(status().isCreated)

        mvc.perform(delete("/expense-core/v1/groups/$groupId/expenses/$expenseId"))
            .andExpect(status().isNoContent)

        mvc.perform(get("/expense-core/v1/groups/$groupId/expenses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
