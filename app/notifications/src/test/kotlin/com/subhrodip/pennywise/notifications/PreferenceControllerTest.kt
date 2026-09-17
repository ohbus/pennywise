package com.subhrodip.pennywise.notifications

import com.subhrodip.pennywise.errors.GlobalErrorHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.security.Principal

class PreferenceControllerTest {
    private val mvc = MockMvcBuilders.standaloneSetup(PreferenceController(InMemoryPreferenceStore()))
        .setControllerAdvice(GlobalErrorHandler()).build()
    private val user = RequestPostProcessor { request -> request.userPrincipal = Principal { "alice" }; request }
    private val blankUser = RequestPostProcessor { request -> request.userPrincipal = Principal { "   " }; request }

    @Test
    fun `reads default preferences for authenticated user`() {
        mvc.perform(get("/notifications/v1/preferences").with(user))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.emailEnabled").value(true))
            .andExpect(jsonPath("$.pushEnabled").value(true))
    }

    @Test
    fun `updates and reads preferences`() {
        mvc.perform(
            put("/notifications/v1/preferences")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailEnabled\":false,\"pushEnabled\":true}")
        )
            .andExpect(status().isNoContent)

        val response = mvc.perform(get("/notifications/v1/preferences").with(user)).andReturn().response
        assertEquals("{\"emailEnabled\":false,\"pushEnabled\":true}", response.contentAsString)
    }

    @Test
    fun `rejects get preferences without authentication`() {
        mvc.perform(get("/notifications/v1/preferences"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects update preferences without authentication`() {
        mvc.perform(
            put("/notifications/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailEnabled\":false,\"pushEnabled\":true}")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects get preferences with blank subject`() {
        mvc.perform(get("/notifications/v1/preferences").with(blankUser))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `rejects update preferences with blank subject`() {
        mvc.perform(
            put("/notifications/v1/preferences")
                .with(blankUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailEnabled\":false,\"pushEnabled\":true}")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }
}
