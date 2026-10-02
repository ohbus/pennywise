package com.subhrodip.squarewise.accounts.auth.audit

/**
 * Enumerated security event types emitted at key authentication lifecycle boundaries.
 *
 * Each event type corresponds to a distinct security-relevant outcome. Consumers of
 * the structured audit log can route, alert, and retain these events independently
 * of general application logs. No credential, raw token, or PII beyond a stable
 * pseudonymous account identifier should appear in emitted event payloads.
 */
enum class SecurityAuditEvent {

    /** A valid magic-link or code was accepted; a new session family was created. */
    LOGIN_SUCCESS,

    /** A submitted credential was invalid, expired, or already redeemed. */
    LOGIN_FAILURE,

    /** A new account identity was explicitly enrolled during first successful login. */
    IDENTITY_ENROLLED,

    /** A login request was denied because the rate-limit policy was exhausted. */
    LOGIN_RATE_LIMITED,

    /** A refresh token was rotated successfully; a new child session was issued. */
    TOKEN_REFRESHED,

    /** A refresh token reuse was detected; the entire family was revoked. */
    TOKEN_REUSE_DETECTED,

    /** A session or refresh family was revoked as a result of logout. */
    SESSION_REVOKED,

    /**
     * A session family was revoked because the resolved account identity does not match
     * the immutable subject bound at session creation time.
     */
    SESSION_SUBJECT_MISMATCH,

    /** A session was denied because the account has a pending deletion request. */
    SESSION_DENIED_DELETION_REQUESTED,

    /** An RSA signing key was rotated; the previous key was added to the historical set. */
    KEY_ROTATED,

    /** An authorization failure occurred on a protected profile or resource endpoint. */
    AUTHORIZATION_DENIED,
}
