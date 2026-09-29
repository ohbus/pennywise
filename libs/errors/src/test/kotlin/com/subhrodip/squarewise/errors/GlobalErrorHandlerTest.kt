package com.subhrodip.squarewise.errors

import com.subhrodip.squarewise.errors.domain.ApplicationException
import com.subhrodip.squarewise.errors.domain.ErrorCode
import com.subhrodip.squarewise.errors.http.GlobalErrorHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.MediaType

class GlobalErrorHandlerTest {

    private val handler = GlobalErrorHandler("test-service")

    @Test
    fun `applicationException maps correctly to ApiProblem`() {
        val ex = ApplicationException(ErrorCode.ERR_05, "Group 123 not found")
        val response = handler.applicationException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(404, body?.status)
        assertEquals("NOT_FOUND", body?.code)
        assertEquals("test-service", body?.source)
        assertEquals("Group 123 not found", body?.detail)
    }

    @Test
    fun `rate limit application error maps to 429 with bounded retry header`() {
        val response = handler.applicationException(ApplicationException(ErrorCode.ERR_11))

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
        assertEquals("RATE_LIMITED", response.body?.code)
        assertEquals("60", response.headers.getFirst("Retry-After"))
    }

    @Test
    fun `all ErrorCode values map to RFC problem schema codes`() {
        val allowedCodes = setOf(
            "VALIDATION_FAILED",
            "UNAUTHENTICATED",
            "FORBIDDEN",
            "NOT_FOUND",
            "CONFLICT",
            "IDEMPOTENCY_CONFLICT",
            "RATE_LIMITED",
            "INTERNAL_ERROR"
        )

        ErrorCode.entries.forEach { code ->
            val ex = ApplicationException(code, "Test error for $code")
            val response = handler.applicationException(ex)
            val problemCode = response.body?.code
            assertTrue(
                allowedCodes.contains(problemCode),
                "Code '$problemCode' for $code must be one of problem.schema.json allowed codes"
            )
        }
    }

    @Test
    fun `illegalArgument maps to BAD_REQUEST and VALIDATION_FAILED`() {
        val response = handler.illegalArgument(IllegalArgumentException("Invalid parameter"))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("VALIDATION_FAILED", response.body?.code)
        assertEquals("Invalid parameter", response.body?.detail)
    }

    @Test
    fun `optimisticLock maps to CONFLICT`() {
        val response = handler.optimisticLock(OptimisticLockingFailureException("Version mismatch"))
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("CONFLICT", response.body?.code)
    }

    @Test
    fun `unexpected exception maps to 500 and INTERNAL_ERROR`() {
        val response = handler.unexpected(RuntimeException("Database connection dropped"))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.code)
    }

    /** Verifies every catalog code keeps its public status and bounded response metadata. */
    @Test
    fun `every catalog error maps to its governed status`() {
        val expectedStatuses = mapOf(
            ErrorCode.ERR_01 to HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.ERR_02 to HttpStatus.BAD_REQUEST,
            ErrorCode.ERR_03 to HttpStatus.UNAUTHORIZED,
            ErrorCode.ERR_04 to HttpStatus.FORBIDDEN,
            ErrorCode.ERR_05 to HttpStatus.NOT_FOUND,
            ErrorCode.ERR_06 to HttpStatus.CONFLICT,
            ErrorCode.ERR_07 to HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.ERR_08 to HttpStatus.BAD_GATEWAY,
            ErrorCode.ERR_09 to HttpStatus.CONFLICT,
            ErrorCode.ERR_10 to HttpStatusCode.valueOf(422),
            ErrorCode.ERR_11 to HttpStatus.TOO_MANY_REQUESTS,
            ErrorCode.ERR_12 to HttpStatus.OK
        )

        expectedStatuses.forEach { (code, status) ->
            val response = handler.applicationException(ApplicationException(code, "safe detail"))

            assertEquals(status, response.statusCode, "Unexpected HTTP status for $code")
            assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.headers.contentType)
            assertEquals("test-service", response.body?.source)
            assertEquals("missing-request-id", response.body?.requestId)
            assertEquals("safe detail", response.body?.detail)
            assertEquals(if (code == ErrorCode.ERR_11) "60" else null, response.headers.getFirst("Retry-After"))
        }
    }

    /** Verifies the content-negotiation failure intentionally has no RFC 7807 body. */
    @Test
    fun `not acceptable response is bodyless`() {
        val response = handler.notAcceptable()

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.statusCode)
        assertEquals(null, response.body)
    }
}
