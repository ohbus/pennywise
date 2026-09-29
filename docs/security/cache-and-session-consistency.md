# Authentication cache and session consistency policy

## Authority rule

PostgreSQL remains authoritative for durable authentication state:

- Provider-qualified identity mappings.
- Refresh-token session families.
- Refresh-token revocation and replacement.
- Logout state.
- Account deletion and security-event revocation.
- Durable audit state.

Redis and in-process caches are accelerators, not authorities for security
decisions.

## Safe cache uses

The existing cache facilities may be used for:

- OIDC discovery metadata.
- JWK material already supported by Spring resource-server validation.
- Distributed rate-limit buckets.
- Bounded, non-authoritative profile projections.
- Request-scoped BFF identity context.
- Configuration-derived policy objects.

Raw refresh tokens, access tokens, cookies, credentials, email addresses, and
unbounded IP values must not be used as cache keys or values.

## Prohibited cache decisions

A stale cache entry must never approve:

- A refresh after session revocation.
- A replaced refresh token.
- A refresh after idle expiry.
- A refresh after absolute expiry.
- A deleted or suspended account.
- A changed provider subject.
- A financial authorization decision.
- A group-membership decision without an explicit version/invalidation policy.

## Database visit policy

Ordinary bearer requests:

```text
local JWT validation -> service-owned authorization/query
```

They must not query Accounts or `auth_sessions` merely to check whether a JWT is
still valid.

Refresh and logout remain writer-authoritative. They should use one secure
transaction and targeted SQL/projections where that reduces round trips without
weakening atomic rotation or revocation.

## Invalidation

Session mutation must invalidate or advance a version for relevant cached data:

- Refresh success.
- Logout.
- Family revocation.
- Account deletion.
- Provider-subject change.
- Suspicious refresh replay.

Cache failure must cause a safe writer fallback or a fail-closed security result.
It must never silently disable revocation or rate limiting.

Accounts bounds Redis connection and command waits to two seconds by default via
`SQUAREWISE_REDIS_CONNECT_TIMEOUT` and `SQUAREWISE_REDIS_COMMAND_TIMEOUT`.
This keeps the fail-closed rate-limit result observable as a bounded `429`
instead of allowing an unavailable cache to hold an authentication request open.

The implemented refresh path now also persists the provider-qualified subject
with each session and compares it with the writer lookup before issuing a child
token. Account deletion uses a single writer bulk update to revoke all active
sessions for the resolved account. Token-bearing login/refresh responses use
`Cache-Control: no-store` and `Pragma: no-cache` so HTTP intermediaries cannot
become an accidental credential cache.

## Required cache tests

- Hit and miss behavior.
- Stale entry after logout.
- Stale entry after family revocation.
- Redis restart.
- Redis unavailable.
- Cache eviction during refresh.
- Writer update followed by cache read.

The executable local matrix is `tests/e2e/test_auth_cache_resilience.py` (also
available as `make e2e-auth-cache`). It deletes only Squarewise rate-limit keys,
fills the refresh admission bucket, verifies eviction restores admission, stops
Redis and expects a bounded fail-closed `429`, then starts Redis and verifies the
normal invalid-token `401` path returns. It must run only against the dedicated
local/CI Compose Redis instance; it must never target a shared environment.
- No raw credential in key/value/log output.
