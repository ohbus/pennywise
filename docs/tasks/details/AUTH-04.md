# AUTH-04 — Validate Keycloak OIDC JWT claims and signatures

## Objective

Configure the four applications to authenticate only with valid signed JWTs
issued by the configured OIDC issuer. Keycloak is the current issuer, but the
implementation must use standard OIDC discovery/JWK and JWT claims so a future
managed provider can be substituted without domain changes.

## Dependencies and ownership

- Depends on `AUTH-03` and `ERR-03`.
- Owns shared decoder/validator code, servlet and reactive adapters, security
  unit tests, and invalid-token contract coverage.
- Keycloak Compose provisioning and real-token journeys are AUTH-06.

## Validation rules

Every protected request must validate:

- signature against issuer-discovered JWKs;
- exact configured issuer;
- configured audience;
- `exp` and `nbf` with bounded clock skew;
- supported asymmetric signing algorithm;
- non-empty, bounded subject;
- bearer token parsing and token type.

Email, preferred username, and display claims must never become the durable
authorization identity. Authorization uses the validated provider-qualified
subject and independently checks group membership in the owning service.

## Required tests

- valid signed token is accepted;
- missing, malformed, unsigned, and bad-signature tokens are rejected;
- wrong issuer and wrong audience are rejected;
- expired and not-yet-valid tokens are rejected;
- unsupported algorithm and missing subject are rejected;
- subject length/format limits are enforced;
- decoder handles JWK key rotation without trusting arbitrary keys;
- servlet and reactive security chains behave equivalently;
- GraphQL HTTP and WebSocket authentication reject invalid/expired tokens;
- invalid credentials do not reach membership, expense, group, or notification
  stores and never appear in errors/logs/metrics.

## Acceptance criteria

- No non-local protected endpoint accepts an arbitrary bearer string.
- All four applications use the same provider-neutral validation policy.
- Keycloak-issued tokens authenticate the subject from `sub` only.
- Wrong issuer, audience, signature, expiry, algorithm, and subject cases have
  executable test evidence.
- Bruno and REST-edge invalid-token cases are added; real Keycloak E2E follows
  AUTH-06.
- Documentation and progress ledger contain exact commands and limitations.
