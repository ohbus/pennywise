# AUTH-07 — Pennywise-owned passwordless authentication contract

## Objective

Define the provider-neutral application flow for low-friction email login.
Pennywise owns the user-facing endpoints and session policy; the configured
OIDC provider remains responsible for identity proof and token signing. Local
Keycloak, Auth0, Okta, and Entra must fit the same port without domain changes.

## Proposed flow

1. `POST /auth/v1/login/start` accepts an email and optional client context.
2. The service always returns the same generic response, regardless of account
   existence, and queues a single-use email link/code when allowed.
3. The browser opens a Pennywise-owned verification route. The credential is
   never logged and is stored only as a hash.
4. `POST /auth/v1/login/verify` atomically redeems the credential and creates a
   Pennywise session or completes the configured provider adapter flow.
5. Browser clients receive a secure HttpOnly session cookie; native clients
   receive a short-lived access token and rotating refresh token.
6. `POST /auth/v1/token/refresh`, `POST /auth/v1/logout`, and session-management
   endpoints enforce rotation, revocation, and reuse detection.

Provider-hosted pages are reserved for MFA, recovery, consent, or step-up
security. Any authorization-code integration uses PKCE `S256`, exact redirect
matching, transaction-bound state/nonce, issuer mix-up protection, and no
implicit or password grants, following RFC 9700.

## Security invariants

- Generic responses prevent email enumeration.
- Email input is normalized once at the authentication boundary: trim outer
  whitespace, apply Unicode-aware case folding, use a canonical international
  domain representation, and validate before lookup or credential issuance.
  The canonical form is used for matching and rate limits; the original
  display form is never used as an identity key.
- Credentials expire, are single-use, hashed, attempt-limited, resend-throttled,
  and atomically redeemed.
- Refresh tokens are hashed, rotated on every use, and revoke their family on
  reuse detection.
- Access tokens are short-lived and audience-restricted.
- Logout, account deletion, provider-subject changes, and suspicious activity
  revoke sessions.
- No credential appears in URLs beyond the one-time link, logs, traces,
  metrics, referrers, or error payloads.
- Every downstream service continues independent resource authorization.

## Acceptance criteria

- Contract is provider-neutral and consistent in local, staging, and production.
- Magic-link and code requests cannot reveal account existence.
- Replay, expiry, brute-force, resend, concurrent redemption, delivery failure,
  refresh replay, logout, and revocation are executable tests.
- REST, GraphQL, and WebSocket clients use the same session/token semantics.
- Keycloak is only a local provider adapter and can be replaced by configuration.

## Status

Registered; contract design is the next implementation gate. No runtime login
endpoint is claimed yet.

## Implementation increment: canonical email value

The first runtime slice adds `auth.identity.EmailAddress` in Accounts. It
trims outer Unicode whitespace, applies Unicode-aware root-locale case folding,
converts the domain to canonical ASCII using IDN rules, and validates bounded
local/domain lengths. The canonical value is suitable for account lookup,
rate-limit keys, and credential issuance. It is not used as the durable
authorization identity, which remains `(issuer, sub)`.

Tests cover normalization, Unicode domain conversion, malformed addresses,
empty components, invalid domain labels, and length limits. Provider adapters,
database migration, login endpoints, and passwordless credential persistence
remain later AUTH-07/AUTH-08 slices.

The provider-neutral endpoint contract is recorded in
`docs/security/passwordless-api-contract.md` before controller or persistence
implementation. It is intentionally application-owned so users remain on a
Pennywise-branded flow; provider-hosted interaction is reserved for MFA,
recovery, consent, or step-up requirements.
