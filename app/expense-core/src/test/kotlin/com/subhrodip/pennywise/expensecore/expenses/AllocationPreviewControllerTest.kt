package com.subhrodip.pennywise.expensecore.expenses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import com.subhrodip.pennywise.errors.GlobalErrorHandler

import com.subhrodip.pennywise.ids.ApiEndpoints

class AllocationPreviewControllerTest {
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(AllocationPreviewController())
        .setControllerAdvice(GlobalErrorHandler()).build()

    @Test
    fun `preview returns exact deterministic allocation as strings`() {
        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.PATH_ALLOCATION_PREVIEW)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"totalMinor":"100","participantIds":["c","a","b"]}""")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMinor").value("100"))
            .andExpect(jsonPath("$.allocations.a").value("34"))
            .andExpect(jsonPath("$.allocations.b").value("33"))
            .andExpect(jsonPath("$.allocations.c").value("33"))
    }

    @Test
    fun `calculator output always sums to request total`() {
        val allocations = AllocationCalculator.equal(101, listOf("a", "b", "c"))
        assertEquals(101L, allocations.values.sum())
    }

    @Test
    fun `invalid participant list returns bad request`() {
        mvc.perform(
            post(ApiEndpoints.ExpenseCore.V1.PATH_ALLOCATION_PREVIEW)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"totalMinor":"100","participantIds":[]}""")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.requestId").value("missing-request-id"))
    }
}
