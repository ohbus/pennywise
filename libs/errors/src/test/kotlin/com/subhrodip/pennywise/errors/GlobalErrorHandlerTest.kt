package com.subhrodip.pennywise.errors

import com.subhrodip.pennywise.errors.domain.ApplicationException
import com.subhrodip.pennywise.errors.domain.ErrorCode
import com.subhrodip.pennywise.errors.http.GlobalErrorHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.dao.OptimisticLockingFailureException

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
}
