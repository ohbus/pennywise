package com.subhrodip.squarewise.accounts.auth.audit

import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * Emits structured, redacted security audit events to the dedicated `security.audit` logger.
 *
 * All emitted log records use a consistent JSON-like key=value format understood by
 * log shippers (Loki, OpenSearch, etc.) and do **not** include:
 * - raw tokens, refresh secrets, or credential plaintext;
 * - raw email addresses (account ID is the stable pseudonymous identifier);
 * - raw IP addresses where prohibited by policy (network partitions use hashed/truncated forms);
 * - full request bodies.
 *
 * Events are emitted at INFO level so they are captured in production log streams without
 * requiring DEBUG mode. The dedicated logger name (`security.audit`) allows operators to route
 * events to an immutable audit sink independently of the main application log stream.
 */
class SecurityAuditLogger {

    private val log = LoggerFactory.getLogger("security.audit")

    /**
     * Emits a security audit event with a stable account identifier and optional correlation ID.
     *
     * @param event Type of the security event.
     * @param accountId Stable pseudonymous account UUID, or null when the account is not yet resolved.
     * @param correlationId Optional request/trace correlation ID for cross-service join.
     * @param detail Optional freeform detail string with no credential or PII content.
     */
    fun emit(
        event: SecurityAuditEvent,
        accountId: UUID? = null,
        correlationId: String? = null,
        detail: String? = null
    ) {
        val ts = Instant.now()
        val sb = StringBuilder("event=${event.name}")
        sb.append(" ts=$ts")
        if (accountId != null) sb.append(" accountId=$accountId")
        if (correlationId != null) sb.append(" correlationId=$correlationId")
        if (detail != null) sb.append(" detail=${detail.take(MAX_DETAIL_LENGTH)}")
        log.info(sb.toString())
    }

    private companion object {
        /** Maximum length for the freeform detail field to bound log line size. */
        const val MAX_DETAIL_LENGTH = 200
    }
}
