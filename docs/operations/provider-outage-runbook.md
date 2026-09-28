# Identity-provider outage runbook

If OIDC discovery or signing-key retrieval is unavailable, fail closed for token
acceptance when validation cannot be completed. Do not substitute arbitrary
bearer values, cached revoked state, or an internal signing key in a production
profile.

For passwordless issuance, an unavailable configured provider must produce a
bounded service error and must not mint a token with fallback production
credentials. Capture provider health, issuer, request correlation ID, and the
time window without logging tokens or secrets.

Restoration requires a fresh health check and a new token; do not reuse a token
obtained during an outage investigation.
