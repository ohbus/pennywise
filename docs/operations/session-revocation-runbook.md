# Session revocation runbook

Logout revokes the refresh-token family identified by the caller's refresh token
after confirming that the authenticated provider-qualified subject owns the
family. Unknown, blank, mismatched, and already-revoked tokens return without
revealing session existence; repeated logout is idempotent.

For account deletion or suspected compromise, use the Accounts writer-backed
family/account revocation workflow and record the event identifier, operator,
timestamp, and affected account. Do not edit `auth_sessions` directly outside an
approved recovery procedure.

After revocation, verify that refresh fails with the generic unauthenticated
error and that no raw refresh token was written to logs or telemetry.
