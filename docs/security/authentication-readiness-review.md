# Authentication readiness review

**Review date:** 2026-09-20  
**Scope:** OIDC resource-server validation, local Keycloak integration,
Pennywise-owned passwordless login, session/token lifecycle, email
normalization, and resource authorization.

## Executive conclusion

The security foundation is directionally sound, but authentication is not yet
end-to-end complete. The current implementation validates externally issued
OIDC access tokens in all four applications when the `local-oidc`, `staging`,
or `production` profile is active. Keycloak is currently a local OIDC provider
fixture and is not embedded in the domain or login policy. That is the correct
provider boundary for future Auth0, Okta, Entra, or another conforming OIDC
provider.

The application-owned passwordless flow is currently a documented contract plus
credential/session persistence and policy building blocks. There is no public
login-start or login-verify controller, no refresh/logout runtime endpoint, no
concrete Accounts-to-Notifications delivery adapter, and no live Keycloak
token-to-application auth journey proving the complete flow. It must therefore
remain `in_progress` and must not be described as production-ready.

## Evidence-based status

| Capability | Current evidence | Assessment |
| --- | --- | --- |
| OIDC issuer/JWK discovery | `libs/security` decoder factories use issuer discovery and standard issuer/time validators | Implemented at library level |
| Audience and subject policy | Required audience and bounded `sub` validators with configurable asymmetric algorithm allow-list | Implemented; add claim-shape and key-rotation integration evidence |
| Local provider | Compose Keycloak 26.7.4, imported realm, PKCE S256, direct grants disabled, discovery observed HTTP 200 | Integrated as a local provider fixture |
| Provider replacement | Issuer/audience/algorithm values are externalized; domain code does not import Keycloak APIs | Good boundary; managed-provider compatibility still needs live-provider contract tests |
| Passwordless email | Canonical `EmailAddress`, HMAC-digested one-time credentials, expiry, single-use conditional redemption, attempt policy | Core building blocks implemented |
| Login orchestration | `LoginStartService` performs rate limit, issue, and delivery-port handoff | Not exposed or Spring-wired as a public use case |
| Email delivery | `AuthEmailSender` outbound port and message model | No concrete event/outbox/Notifications adapter yet |
| Access tokens | Resource services validate bearer JWTs | No application-issued access-token endpoint yet |
| Refresh tokens | Session schema and rotation repository primitives exist | No runtime refresh/reuse-detection service or endpoint yet |
| Resource authorization | Group/expense operations accept authenticated principal and membership checks exist in Expense Core | Must be audited operation-by-operation; authorization must remain service-owned and never rely on BFF filtering |
| Contract/E2E | Passwordless behavior exists in Markdown only; REST contracts contain no auth operations | Missing executable public-surface, Bruno, and E2E evidence |

## Keycloak decision

Keycloak should remain a local and test-environment OIDC provider, not a
Pennywise domain dependency. Pennywise should own the user experience and its
application authorization model. The provider adapter should only supply
identity proof, issuer metadata, signing keys, and—when required—provider-hosted
step-up/MFA/consent interaction.

The replacement seam is the OIDC configuration and validation port, not a
Keycloak-specific service API. A future provider change should require only
issuer/client/redirect/claim mapping configuration plus adapter tests. The
following must not appear in domain or authorization code: Keycloak admin APIs,
Keycloak token classes, realm-role assumptions, provider-specific group names,
or email-as-identity matching.

## Recommended user journeys

### Browser

1. The client posts a normalized email to the Pennywise-owned login-start
   endpoint.
2. Pennywise always returns the same accepted response and queues a single-use
   link. Unknown accounts are indistinguishable from known accounts.
3. The link opens a Pennywise-branded verification route. The credential is
   redeemed once, then removed from the URL before any third-party request or
   analytics event.
4. Pennywise creates a server-side session and sends a `Secure`, `HttpOnly`,
   `SameSite=Lax` cookie with bounded idle/absolute expiry. CSRF protection is
   required for cookie-authenticated state-changing requests.

### Native/client API

1. Login-start and code verification use the same application endpoints.
2. Verification returns a short-lived, audience-restricted access token and a
   rotating opaque refresh token.
3. Refresh rotates the token on every successful use. Reuse of an old token
   revokes the entire family and requires a new login.
4. Logout revokes the current session/family according to an explicit endpoint
   policy; account deletion and suspicious activity revoke all sessions.

Provider authorization-code + PKCE remains available for clients or step-up
flows that need provider-managed MFA/consent. It must use authorization code,
PKCE S256, exact redirect allow-lists, state and nonce bound to the transaction,
issuer mix-up protection, and no implicit or password grant.

## High-priority gaps and implementation plan

1. **AUTH-07A — Public auth contract and controllers.** Add OpenAPI schemas and
   endpoints for start, verify-link/code, refresh, logout, and session
   revocation. Wire `LoginStartService` with trusted server-derived network
   partitioning. Define cookie versus native response semantics and generic
   error behavior.
2. **AUTH-07B — Delivery integration.** Publish an Accounts-owned transactional
   auth-email event/outbox message. Consume it in Notifications with a typed
   adapter to the existing SMTP/Mailpit port. Ensure retries are idempotent and
   plaintext credentials never enter logs, broker diagnostics, traces, or
   persisted outbox payloads beyond the minimum encrypted/delivery boundary.
3. **AUTH-07C — Verification and token service.** Implement atomic credential
   verification, provider-subject/account linking, access-token issuance, refresh
   rotation, reuse detection, logout, and family/session revocation. Keep token
   signing behind a replaceable port and use a key identifier plus rotation
   procedure.
4. **AUTH-07D — Authorization proof.** Make every group/expense command and
   read operation derive the subject from the validated token and perform a
   service-local membership/role check. Add negative tests for a valid token
   belonging to a non-member, removed member, archived group, wrong group ID,
   forged participant IDs, and BFF bypass/direct-service access.
5. **AUTH-07E — Executable security evidence.** Add unit, Spring persistence,
   PostgreSQL concurrency, controller, Bruno, and live Compose tests for replay,
   expiry, brute force, resend cooldown, enumeration resistance, delivery
   failure, refresh replay, logout, cookie flags, CSRF, issuer/audience/key
   rotation, and Keycloak authorization-code + PKCE.
6. **AUTH-07F — Provider portability gate.** Add a mock OIDC discovery/JWK
   contract fixture and a second non-Keycloak-compatible claim fixture. Verify
   that only configuration and provider adapters change when issuer, JWKS, and
   optional claims differ.

## Delivery boundary decision

The existing Expense Core outbox is service-private and must not be imported by
Accounts. Auth-email delivery will use an Accounts-owned transactional outbox
with a versioned `auth.email.requested.v1` event. Accounts will persist the
credential digest and delivery record in one transaction; the raw credential
must not be persisted in the outbox or generic notification inbox. The event
adapter requires an explicitly bounded protected handoff for the one-time
plaintext, with strict redaction and short retention. Notifications consumes
the versioned event and delegates SMTP/provider delivery to its existing
`EmailDispatcher` port.

## Acceptance gate for calling authentication production-ready

The goal is not met until all of the following have executable evidence:

- A complete local Keycloak journey obtains a real signed token and exercises
  Accounts, Expense Core, BFF GraphQL, and WebSocket behavior.
- Pennywise-owned email link and code journeys work without redirecting users to
  an unknown provider page for ordinary login.
- Access tokens are short-lived, refresh tokens are opaque, stored only as
  digests, rotated on every use, and family-revoked on replay.
- Every protected resource denies authenticated non-members and never trusts
  client-supplied user IDs, email addresses, or BFF-only checks.
- Local, staging, and production use the same flow and fail closed when issuer,
  audience, signing policy, digest secret, or token-signing configuration is
  absent.
- REST/OpenAPI, GraphQL, Bruno, unit, persistence, concurrency, and E2E
  artifacts are synchronized with the tracker and progress ledger.
