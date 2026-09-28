package com.subhrodip.pennywise.errors.domain

/**
 * Enum representing error codes defined in the error catalog.
 * Each entry maps to a code, HTTP status, severity and safe detail message.
 */
enum class ErrorCode(val code: String, val httpStatus: Int, val severity: String, val safeDetail: String) {
    ERR_01("ERR-01", 500, "critical", "Internal server error"),
    ERR_02("ERR-02", 400, "error", "Invalid request parameters"),
    ERR_03("ERR-03", 401, "error", "Authentication required"),
    ERR_04("ERR-04", 403, "error", "Access denied"),
    ERR_05("ERR-05", 404, "warning", "Resource not found"),
    ERR_06("ERR-06", 409, "error", "Duplicate resource"),
    ERR_07("ERR-07", 500, "error", "Notification processing error"),
    ERR_08("ERR-08", 502, "error", "Upstream service failure"),
    ERR_09("ERR-09", 409, "error", "Sync conflict"),
    ERR_10("ERR-10", 422, "error", "Insufficient funds"),
    ERR_11("ERR-11", 429, "warning", "Rate limit exceeded"),
    ERR_12("ERR-12", 200, "info", "Governance completed");
}



/**
 * Helper to create a Problem Details response (RFC 7807) from an [ApplicationException].
 */
fun ApplicationException.toProblemDetails(requestId: String): Map<String, Any> = mapOf(
    "type" to "https://example.com/problems/${errorCode.code}",
    "title" to errorCode.safeDetail,
    "status" to errorCode.httpStatus,
    "detail" to (message ?: errorCode.safeDetail),
    "instance" to requestId,
    "errorId" to java.util.UUID.randomUUID().toString(),
    "errorCode" to errorCode.code
)
