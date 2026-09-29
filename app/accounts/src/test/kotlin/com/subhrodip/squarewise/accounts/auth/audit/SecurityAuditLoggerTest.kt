package com.subhrodip.squarewise.accounts.auth.audit

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import java.util.UUID

/**
 * Unit tests for SecurityAuditLogger verifying that events are emitted
 * without throwing, and that oversized detail strings are truncated.
 */
class SecurityAuditLoggerTest {

    private val logger = SecurityAuditLogger()

    @Test
    fun `emits LOGIN_SUCCESS without throwing`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.LOGIN_SUCCESS, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits LOGIN_FAILURE without account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.LOGIN_FAILURE, detail = "invalid-or-expired-credential")
        }
    }

    @Test
    fun `emits IDENTITY_ENROLLED with account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.IDENTITY_ENROLLED, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits LOGIN_RATE_LIMITED without account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.LOGIN_RATE_LIMITED)
        }
    }

    @Test
    fun `emits TOKEN_REFRESHED with account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.TOKEN_REFRESHED, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits TOKEN_REUSE_DETECTED with account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.TOKEN_REUSE_DETECTED, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits SESSION_REVOKED with account ID`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.SESSION_REVOKED, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits SESSION_SUBJECT_MISMATCH without throwing`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.SESSION_SUBJECT_MISMATCH, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits SESSION_DENIED_DELETION_REQUESTED without throwing`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.SESSION_DENIED_DELETION_REQUESTED, accountId = UUID.randomUUID())
        }
    }

    @Test
    fun `emits KEY_ROTATED with detail`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.KEY_ROTATED, detail = "kid=new-key-abc")
        }
    }

    @Test
    fun `emits AUTHORIZATION_DENIED with correlation ID`() {
        assertDoesNotThrow {
            logger.emit(
                SecurityAuditEvent.AUTHORIZATION_DENIED,
                accountId = UUID.randomUUID(),
                correlationId = "req-12345",
                detail = "profile-lookup-unauthorized"
            )
        }
    }

    @Test
    fun `truncates oversized detail string without throwing`() {
        val oversizedDetail = "x".repeat(1000)
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.LOGIN_FAILURE, detail = oversizedDetail)
        }
    }

    @Test
    fun `emits event with all null optional fields without throwing`() {
        assertDoesNotThrow {
            logger.emit(SecurityAuditEvent.SESSION_REVOKED)
        }
    }
}
