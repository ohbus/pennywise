# Endpoint authentication matrix

## Purpose

This document is the required inventory for authentication and authorization
coverage. It prevents a newly added endpoint from inheriting an accidental
security default and keeps REST, GraphQL, and WebSocket behavior aligned.

The registry and task detail remain authoritative for ownership and status. This
matrix is the implementation checklist and evidence index.

## Global policy

Every endpoint is either explicitly public or explicitly authenticated. A valid
bearer token proves identity only; each service still performs resource-level
authorization. The BFF cannot grant access that an upstream service would deny.

Ordinary JWT requests validate issuer, audience, signature, temporal claims,
algorithm, token type, and bounded provider-qualified subject locally. They do
not query Accounts or the session database merely to validate a bearer token.

## Public endpoint classes

| Class | Authentication | Additional controls |
|---|---|---|
| Liveness | Public | No sensitive details |
| Readiness | Public or private by deployment policy | Dependency state is bounded and redacted |
| Login start | Public | Canonicalization, anti-enumeration, rate limit |
| Login verify | Public | Single-use credential, expiry, attempt limit, rate limit |
| Token refresh | Public transport boundary | Opaque refresh token, rotation, replay detection, rate limit |
| OIDC discovery | Provider boundary | HTTPS and issuer allow-list |
| Metrics/diagnostics | Restricted | Network and role protection; no credentials |

## Protected endpoint classes

| Class | Required checks |
|---|---|
| Account profile | Authenticated subject and account ownership |
| Export/deletion | Authenticated subject, account state, rate policy |
| Expense groups | Authenticated subject and active membership |
| Expense mutations | Membership, archived state, idempotency, revision |
| Settlements | Membership, participant scope, idempotency |
| Invitations | Membership and invite-token policy |
| Synchronization | Membership, group identity, cursor scope |
| Notifications | Authenticated subject and inbox ownership |
| GraphQL HTTP | Valid bearer and resolver/upstream authorization |
| WebSocket handshake | Valid bearer, origin policy, subscription authorization |

## Required negative matrix

Every protected route or operation must have evidence for:

- Missing authorization header.
- Malformed bearer scheme.
- Empty token.
- Forged signature.
- Unsupported algorithm.
- Wrong issuer.
- Wrong audience.
- Expired token.
- Not-yet-valid token.
- Blank subject.
- Overlong subject.
- Authenticated but unauthorized account.
- Non-member.
- Removed member.
- Archived resource.
- Cross-group identifier.
- Rate-limit denial.
- Upstream dependency failure.
- Timeout.
- Cache unavailable where applicable.

## Evidence status

The matrix must be populated per operation during AUTH-08 implementation. A
route cannot be marked complete from a shared filter test alone; it requires
transport, authentication, resource authorization, and failure evidence at the
appropriate test level.
