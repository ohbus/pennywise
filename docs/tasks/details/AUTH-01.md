# AUTH-01 — Provider-neutral authentication hardening baseline

## Objective

Define and register the authentication architecture before implementing login,
token, or provider-specific integrations. The application must support local
OIDC validation with Keycloak while remaining configurable for Auth0, Okta,
Microsoft Entra ID, or another standards-compliant OIDC provider.

## Decision

Pennywise will not depend on Keycloak-specific APIs or claims. Services consume
validated OIDC identities through Spring Security resource-server support.
Keycloak is an optional local identity provider and test fixture, not an
application runtime dependency. Provider-specific issuer, audience, discovery,
client, and email settings are deployment configuration.

The user-facing login surface remains Pennywise-owned. Passwordless email links
and one-time codes may be implemented behind an OIDC/provider adapter. Provider
hosted pages are reserved for consent, MFA, recovery, or other step-up flows.

Access credentials must use short-lived access tokens and rotating refresh
tokens, or an equivalent secure server-side session for browser clients.

## Initial findings

- The `local` profile accepts any bearer value as the subject and performs no
  signature, issuer, audience, or expiry validation.
- No Keycloak container, realm, issuer configuration, or real OIDC acceptance
  flow is currently present.
- Production OIDC resource-server wiring is documented but not yet evidenced
  by a production configuration or integration test.
- `ExpenseController` contains a `test-user` fallback when the principal is
  absent; this must be removed before production authentication work is
  considered complete.

## Planned child increments

1. Remove implicit identities and make missing authentication a hard 401.
2. Add fail-closed production JWT resource-server configuration with issuer,
   audience, algorithm, expiry, and subject validation.
3. Add explicit opt-in and localhost-only safeguards for passthrough local auth.
4. Add optional local Keycloak, realm bootstrap, and Mailpit integration.
5. Add provider-neutral login-start, callback, email-link, and one-time-code
   contracts with anti-enumeration and rate limiting.
6. Add identity mapping, short-lived access credentials, refresh rotation,
   reuse detection, logout, revocation, and session management.
7. Add HTTP, GraphQL, WebSocket, provider-compatibility, and operational tests.

## Acceptance criteria

- No non-local environment accepts arbitrary bearer tokens.
- Production startup fails closed when OIDC configuration is absent or invalid.
- Local Keycloak issues real tokens consumed by all four applications.
- A generic OIDC provider can be selected through configuration without domain
  or authorization code changes.
- Passwordless login credentials are single-use, expiring, rate-limited, and
  never logged or stored in plaintext.
- Access and refresh credential lifecycle behavior is covered by executable
  tests, including rotation, reuse detection, logout, and revocation.
- Domain services continue to enforce membership authorization independently.

## Validation commands

- `python3 tools/contracts/validate.py`
- `python3 tools/contracts/validate_public_surface.py`
- `./gradlew test --rerun-tasks --no-daemon`
- `make security-hygiene`
- `make check`
- `git diff --check`

## Status

Planned. No authentication implementation is claimed by this task.
