package com.subhrodip.squarewise.bff.graphql

import com.subhrodip.squarewise.bff.transport.UpstreamServiceException
import com.subhrodip.squarewise.errors.domain.ApplicationException
import com.subhrodip.squarewise.errors.domain.ErrorCode
import graphql.language.Field
import graphql.execution.ExecutionStepInfo
import graphql.execution.ResultPath
import graphql.Scalars
import graphql.schema.DataFetchingEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.graphql.execution.ErrorType

/** Verifies the GraphQL resolver's complete public failure classification matrix. */
class GraphQlExceptionResolverTest {
    private val resolver = ExposedResolver()
    private val environment = mock(DataFetchingEnvironment::class.java).apply {
        `when`(getField()).thenReturn(Field("test"))
        `when`(getExecutionStepInfo()).thenReturn(
            ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLString)
                .path(ResultPath.rootPath())
                .build()
        )
    }

    @Test
    fun `maps every application error code to its public GraphQL contract`() {
        ErrorCode.entries.forEach { errorCode ->
            val error = resolver.resolve(ApplicationException(errorCode), environment)

            assertEquals(expectedGraphQlCode(errorCode), error.extensions?.get("code"))
            assertEquals(errorCode.safeDetail, error.message)
            assertEquals(expectedClassification(errorCode), error.errorType)
            assertNotNull(error.extensions?.get("requestId"))
            if (errorCode == ErrorCode.ERR_11) {
                assertEquals(60, error.extensions?.get("retryAfterSeconds"))
            } else {
                assertNull(error.extensions?.get("retryAfterSeconds"))
            }
        }
    }

    @Test
    fun `maps every upstream status and uses internal default for unknown status`() {
        val expected = mapOf(
            400 to "VALIDATION_FAILED",
            401 to "UNAUTHENTICATED",
            403 to "FORBIDDEN",
            404 to "NOT_FOUND",
            409 to "CONFLICT",
            422 to "VALIDATION_FAILED",
            429 to "RATE_LIMITED",
            500 to "INTERNAL_ERROR",
            503 to "INTERNAL_ERROR"
        )

        expected.forEach { (status, code) ->
            val error = resolver.resolve(UpstreamServiceException(status), environment)
            assertEquals(code, error.extensions?.get("code"))
            assertNotNull(error.extensions?.get("requestId"))
            if (status == 429) assertEquals(60, error.extensions?.get("retryAfterSeconds"))
        }
    }

    @Test
    fun `maps argument and unknown failures to safe validation and internal errors`() {
        val argumentError = resolver.resolve(IllegalArgumentException("secret detail"), environment)
        assertEquals("VALIDATION_FAILED", argumentError.extensions?.get("code"))
        assertEquals(ErrorType.BAD_REQUEST, argumentError.errorType)
        assertEquals(ErrorCode.ERR_02.safeDetail, argumentError.message)

        val unknownError = resolver.resolve(IllegalStateException("secret detail"), environment)
        assertEquals("INTERNAL_ERROR", unknownError.extensions?.get("code"))
        assertEquals(ErrorType.INTERNAL_ERROR, unknownError.errorType)
        assertEquals(ErrorCode.ERR_01.safeDetail, unknownError.message)
    }

    private fun expectedGraphQlCode(errorCode: ErrorCode): String = when (errorCode) {
        ErrorCode.ERR_01, ErrorCode.ERR_07, ErrorCode.ERR_08 -> "INTERNAL_ERROR"
        ErrorCode.ERR_02, ErrorCode.ERR_10 -> "VALIDATION_FAILED"
        ErrorCode.ERR_03 -> "UNAUTHENTICATED"
        ErrorCode.ERR_04 -> "FORBIDDEN"
        ErrorCode.ERR_05 -> "NOT_FOUND"
        ErrorCode.ERR_06, ErrorCode.ERR_09 -> "CONFLICT"
        ErrorCode.ERR_11 -> "RATE_LIMITED"
        ErrorCode.ERR_12 -> "GOVERNANCE_COMPLETED"
    }

    private fun expectedClassification(errorCode: ErrorCode): ErrorType = when (errorCode) {
        ErrorCode.ERR_03 -> ErrorType.UNAUTHORIZED
        ErrorCode.ERR_04 -> ErrorType.FORBIDDEN
        ErrorCode.ERR_05 -> ErrorType.NOT_FOUND
        ErrorCode.ERR_01, ErrorCode.ERR_07, ErrorCode.ERR_08 -> ErrorType.INTERNAL_ERROR
        else -> ErrorType.BAD_REQUEST
    }

    private class ExposedResolver : GraphQlExceptionResolver() {
        fun resolve(exception: Throwable, environment: DataFetchingEnvironment) =
            resolveToSingleError(exception, environment)
    }
}
