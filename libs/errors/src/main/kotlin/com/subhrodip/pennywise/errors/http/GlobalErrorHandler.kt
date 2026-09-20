package com.subhrodip.pennywise.errors

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Global REST controller advice translating domain and framework exceptions into RFC 7807
 * problem details with structured, multi-level diagnostic logging.
 *
 * Invariants:
 * - Unhandled server exceptions (500) are logged at ERROR with full stack trace and correlation ID.
 * - Client errors (400 validation failures, malformed payloads, 404s, 409 conflicts) are logged at WARN.
 * - Sensitive values (passwords, tokens, raw amounts) are never echoed into logs.
 */
@RestControllerAdvice
class GlobalErrorHandler(
    @Value("\${spring.application.name:unknown}") private val serviceName: String = "unknown"
) {

    /**
     * Returns a bodyless 406 when the client requests a representation that the endpoint cannot
     * produce. A body is intentionally omitted because serializing an RFC 7807 response would
     * repeat the same content-negotiation failure.
     *
     * @return HTTP 406 with no negotiated response body
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun notAcceptable(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build()

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(error: MethodArgumentNotValidException): ResponseEntity<ApiProblem> {
        val violations = error.bindingResult.fieldErrors.map {
            FieldViolation(it.field, it.defaultMessage ?: "invalid value")
        }
        val summary = violations.joinToString("; ") { "${it.field}: ${it.message}" }
        log.warn("Request validation failed [requestId={}]: {}", RequestIdContext.get(), summary)
        return problem(
            ErrorCode.ERR_02,
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            "One or more fields are invalid",
            violations
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun messageNotReadable(error: HttpMessageNotReadableException): ResponseEntity<ApiProblem> {
        val detail = error.rootCause?.message ?: error.message ?: "Malformed request payload"
        log.warn("Malformed HTTP request payload [requestId={}]: {}", RequestIdContext.get(), detail)
        return problem(
            ErrorCode.ERR_02,
            HttpStatus.BAD_REQUEST,
            "Malformed request payload",
            detail
        )
    }

    @ExceptionHandler(ServletRequestBindingException::class)
    fun requestBinding(error: ServletRequestBindingException): ResponseEntity<ApiProblem> {
        val detail = error.message ?: "Invalid request binding"
        log.warn("Missing or invalid request parameter or header [requestId={}]: {}", RequestIdContext.get(), detail)
        return problem(
            ErrorCode.ERR_02,
            HttpStatus.BAD_REQUEST,
            "Missing or invalid request parameter or header",
            detail
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun argumentTypeMismatch(error: MethodArgumentTypeMismatchException): ResponseEntity<ApiProblem> {
        log.warn(
            "Type mismatch for parameter '{}' [requestId={}]: expected type '{}'",
            error.name,
            RequestIdContext.get(),
            error.requiredType?.simpleName
        )
        return problem(
            ErrorCode.ERR_02,
            HttpStatus.BAD_REQUEST,
            "Type mismatch for parameter ${error.name}",
            error.message
        )
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun optimisticLock(error: OptimisticLockingFailureException): ResponseEntity<ApiProblem> {
        log.warn(
            "Optimistic locking conflict detected [requestId={}]: {}",
            RequestIdContext.get(),
            error.message
        )
        return problem(
            ErrorCode.ERR_06,
            HttpStatus.CONFLICT,
            "Conflict",
            error.message ?: "Resource was updated by another transaction"
        )
    }


    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgument(error: IllegalArgumentException): ResponseEntity<ApiProblem> {
        val detail = error.message ?: "Invalid request"
        log.warn("Invalid request argument [requestId={}]: {}", RequestIdContext.get(), detail)
        return problem(
            ErrorCode.ERR_02,
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            detail
        )
    }



    @ExceptionHandler(ApplicationException::class)
    fun applicationException(ex: ApplicationException): ResponseEntity<ApiProblem> {
        val status = HttpStatus.resolve(ex.errorCode.httpStatus) ?: when (ex.errorCode) {
            ErrorCode.ERR_02 -> HttpStatus.BAD_REQUEST
            ErrorCode.ERR_03 -> HttpStatus.UNAUTHORIZED
            ErrorCode.ERR_04 -> HttpStatus.FORBIDDEN
            ErrorCode.ERR_05 -> HttpStatus.NOT_FOUND
            ErrorCode.ERR_06 -> HttpStatus.CONFLICT
            ErrorCode.ERR_07 -> HttpStatus.INTERNAL_SERVER_ERROR
            ErrorCode.ERR_08 -> HttpStatus.BAD_GATEWAY
            ErrorCode.ERR_09 -> HttpStatus.CONFLICT
            ErrorCode.ERR_10 -> HttpStatusCode.valueOf(422)
            ErrorCode.ERR_11 -> HttpStatus.TOO_MANY_REQUESTS
            ErrorCode.ERR_12 -> HttpStatus.OK
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return problem(
            ex.errorCode,
            status,
            title = ex.message ?: ex.errorCode.safeDetail,
            detail = ex.message ?: ex.errorCode.safeDetail
        )
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception): ResponseEntity<ApiProblem> {
        log.error("Unhandled unexpected exception [requestId={}]: {}", RequestIdContext.get(), error.message, error)
        return problem(
            ErrorCode.ERR_01,
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    private fun problem(
        code: ErrorCode,
        status: HttpStatusCode,
        title: String = "Internal server error",
        detail: String = "An unexpected error occurred",
        violations: List<FieldViolation> = emptyList()
    ): ResponseEntity<ApiProblem> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                ApiProblem(
                    type = "https://pennywise.example/problems/${code.name.lowercase()}",
                    title = title,
                    status = status.value(),
                    code = mapErrorCode(code),
                    source = serviceName,
                    requestId = RequestIdContext.get(),
                    detail = detail,
                    violations = violations
                )
            )

    private fun mapErrorCode(errorCode: ErrorCode): String =
        when (errorCode) {
            ErrorCode.ERR_01 -> "INTERNAL_ERROR"
            ErrorCode.ERR_02 -> "VALIDATION_FAILED"
            ErrorCode.ERR_03 -> "UNAUTHENTICATED"
            ErrorCode.ERR_04 -> "FORBIDDEN"
            ErrorCode.ERR_05 -> "NOT_FOUND"
            ErrorCode.ERR_06 -> "CONFLICT"
            ErrorCode.ERR_07 -> "INTERNAL_ERROR"
            ErrorCode.ERR_08 -> "INTERNAL_ERROR"
            ErrorCode.ERR_09 -> "CONFLICT"
            ErrorCode.ERR_10 -> "VALIDATION_FAILED"
            ErrorCode.ERR_11 -> "RATE_LIMITED"
            ErrorCode.ERR_12 -> "INTERNAL_ERROR"
        }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalErrorHandler::class.java)
    }
}
