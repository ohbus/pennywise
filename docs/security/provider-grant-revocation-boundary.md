# Provider grant revocation boundary

AUTH-08 uses Squarewise-owned opaque refresh sessions. The external OIDC
provider supplies issuer identity, discovery/JWK validation, and signed access
tokens; it does not issue or store the refresh-token family represented by
`auth_sessions`.

Therefore logout, reuse detection, account deletion, and subject-remap events
revoke the PostgreSQL session family directly. There is no provider refresh
token handle or provider grant identifier in the current contract, so an RFC
7009 provider-revocation call would not identify the session being revoked and
must not be fabricated.

If a future deployment delegates refresh grants to an identity provider, it
must add a provider-owned grant handle, an explicit revocation adapter, bounded
timeouts, fail-closed security-event behavior, and provider contract tests
before enabling that mode. The current application-owned flow does not claim
that future integration.
