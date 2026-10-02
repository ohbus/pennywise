package com.subhrodip.squarewise.errors

import com.subhrodip.squarewise.errors.domain.ApplicationException
import com.subhrodip.squarewise.errors.domain.ErrorCode
import com.subhrodip.squarewise.errors.domain.toProblemDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/** Verifies the legacy problem-details adapter preserves catalog and correlation semantics. */
class ErrorCodeProblemDetailsTest {

    /** A supplied application message is exposed as detail while catalog metadata remains stable. */
    @Test
    fun `problem details preserve supplied message and catalog metadata`() {
        val details = ApplicationException(ErrorCode.ERR_05, "group is missing")
            .toProblemDetails("request-42")

        assertEquals("https://example.com/problems/ERR-05", details["type"])
        assertEquals("Resource not found", details["title"])
        assertEquals(404, details["status"])
        assertEquals("group is missing", details["detail"])
        assertEquals("request-42", details["instance"])
        assertEquals("ERR-05", details["errorCode"])
        assertNotNull(details["errorId"])
    }

    /** A missing application message falls back to the catalog-safe detail. */
    @Test
    fun `problem details use safe catalog detail when message is absent`() {
        val details = ApplicationException(ErrorCode.ERR_11).toProblemDetails("request-43")

        assertEquals("Rate limit exceeded", details["detail"])
        assertEquals(429, details["status"])
        assertEquals("ERR-11", details["errorCode"])
    }

    /** A provider that supplies no exception message still receives the safe catalog detail. */
    @Test
    fun `problem details tolerate a null exception message`() {
        val exception = Mockito.mock(ApplicationException::class.java)
        Mockito.`when`(exception.errorCode).thenReturn(ErrorCode.ERR_02)
        Mockito.`when`(exception.message).thenReturn(null)

        val details = exception.toProblemDetails("request-44")

        assertEquals("Invalid request parameters", details["detail"])
        assertEquals("ERR-02", details["errorCode"])
    }
}
