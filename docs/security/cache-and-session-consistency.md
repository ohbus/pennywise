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

## Required cache tests

- Hit and miss behavior.
- Stale entry after logout.
- Stale entry after family revocation.
- Redis restart.
- Redis unavailable.
- Cache eviction during refresh.
- Writer update followed by cache read.
- No raw credential in key/value/log output.
