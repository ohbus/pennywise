package com.subhrodip.squarewise.errors.request

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/** Verifies request correlation IDs are propagated and never leak between requests. */
class RequestIdContextAndFilterTest {

    private val filter = RequestIdFilter()

    /** A valid client correlation ID is reflected and visible inside the request context. */
    @Test
    fun `valid request id is propagated through the filter`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/health"
            addHeader(RequestIdFilter.HEADER, "client-request-42")
        }
        val response = MockHttpServletResponse()
        var observedContext = ""
        val chain = FilterChain { _, _ -> observedContext = RequestIdContext.get() }

        filter.doFilter(request, response, chain)

        assertEquals("client-request-42", response.getHeader(RequestIdFilter.HEADER))
        assertEquals("client-request-42", observedContext)
        assertEquals("missing-request-id", RequestIdContext.get())
    }

    /** Invalid and oversized IDs are replaced with a bounded generated UUID. */
    @Test
    fun `invalid request id is replaced and context is cleaned`() {
        val request = MockHttpServletRequest().apply {
            method = "POST"
            requestURI = "/groups"
            addHeader(RequestIdFilter.HEADER, "contains spaces")
        }
        val response = MockHttpServletResponse()
        var observedContext = ""
        val chain = FilterChain { _, _ -> observedContext = RequestIdContext.get() }

        filter.doFilter(request, response, chain)

        val responseId = response.getHeader(RequestIdFilter.HEADER)
        assertNotEquals("contains spaces", responseId)
        assertTrue(responseId?.matches(Regex("[0-9a-f-]{36}")) == true)
        assertEquals(responseId, observedContext)
        assertEquals("missing-request-id", RequestIdContext.get())
    }

    /** Nested context scopes restore no stale value after success or failure. */
    @Test
    fun `context cleanup runs when action throws`() {
        val failure = IllegalStateException("boom")

        try {
            RequestIdContext.with("request-1") {
                assertEquals("request-1", RequestIdContext.get())
                throw failure
            }
        } catch (actual: IllegalStateException) {
            assertEquals(failure, actual)
        }

        assertEquals("missing-request-id", RequestIdContext.get())
    }
}
