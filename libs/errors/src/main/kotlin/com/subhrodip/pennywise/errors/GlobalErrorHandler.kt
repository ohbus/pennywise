package com.subhrodip.pennywise.errors

import org.slf4j.LoggerFactory
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

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
class GlobalErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(error: MethodArgumentNotValidException): ResponseEntity<ApiProblem> {
        val violations = error.bindingResult.fieldErrors.map {
            FieldViolation(it.field, it.defaultMessage ?: "invalid value")
        }
        val summary = violations.joinToString("; ") { "${it.field}: ${it.message}" }
        log.warn("Request validation failed [requestId={}]: {}", RequestIdContext.get(), summary)
        return problem(
            ErrorCode.VALIDATION_FAILED,
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
            ErrorCode.VALIDATION_FAILED,
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
            ErrorCode.VALIDATION_FAILED,
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
            ErrorCode.VALIDATION_FAILED,
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
            ErrorCode.CONFLICT,
            HttpStatus.CONFLICT,
            "Conflict",
            error.message ?: "Resource was updated by another transaction"
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun responseStatus(error: ResponseStatusException): ResponseEntity<ApiProblem> {
        val code = when (error.statusCode.value()) {
            401 -> ErrorCode.UNAUTHENTICATED
            403 -> ErrorCode.FORBIDDEN
            404 -> ErrorCode.NOT_FOUND
            409 -> ErrorCode.CONFLICT
            429 -> ErrorCode.RATE_LIMITED
            else -> ErrorCode.VALIDATION_FAILED
        }
        val detail = error.reason ?: "Request rejected"
        if (error.statusCode.is4xxClientError) {
            log.warn("Client request rejected HTTP {} code={} [requestId={}]: {}", error.statusCode.value(), code, RequestIdContext.get(), detail)
        } else {
            log.error("Server error HTTP {} code={} [requestId={}]: {}", error.statusCode.value(), code, RequestIdContext.get(), detail, error)
        }
        return problem(code, error.statusCode, error.statusCode.toString(), detail)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgument(error: IllegalArgumentException): ResponseEntity<ApiProblem> {
        val detail = error.message ?: "Invalid request"
        log.warn("Invalid request argument [requestId={}]: {}", RequestIdContext.get(), detail)
        return problem(
            ErrorCode.VALIDATION_FAILED,
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            detail
        )
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception): ResponseEntity<ApiProblem> {
        log.error("Unhandled unexpected exception [requestId={}]: {}", RequestIdContext.get(), error.message, error)
        return problem(
            ErrorCode.INTERNAL_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred"
        )
    }

    private fun problem(
        code: ErrorCode,
        status: HttpStatusCode,
        title: String,
        detail: String,
        violations: List<FieldViolation> = emptyList()
    ): ResponseEntity<ApiProblem> =
        ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                ApiProblem(
                    type = "https://pennywise.example/problems/${code.name.lowercase()}",
                    title = title,
                    status = status.value(),
                    code = code,
                    requestId = RequestIdContext.get(),
                    detail = detail,
                    violations = violations
                )
            )

    companion object {
        private val log = LoggerFactory.getLogger(GlobalErrorHandler::class.java)
    }
}
