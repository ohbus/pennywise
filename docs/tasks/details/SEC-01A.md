# SEC-01A — Establish operational token authority

## Status

Planned. Prerequisite for SEC-01B through SEC-01F.

## Objective

Resolve findings SEC-001 and SEC-009 by implementing an operational, production-ready
token authority in Accounts and removing the throwing `ExternalOidcTokenProvider` stub
from production/staging/local-oidc configurations.

Squarewise adopts the **Squarewise-owned asymmetric signing authority** model:
- Accounts acts as an asymmetric JWT signer (using RSA RS256 with key ID `kid`).
- Accounts publishes public keys via a standard JWKS endpoint (`/.well-known/jwks.json`
  and `/auth/jwks.json`).
- Deployed passwordless verification (`LoginVerificationService`) issues valid signed
  JWT access tokens and opaque refresh session tokens.
- Downstream resource servers (Expense Core, Notifications, BFF, and Accounts itself)
  validate tokens locally against the JWKS endpoint without hitting the Accounts database.
- Key rotation is supported via multi-key JWKS publishing and key identifier resolution.
- External provider integration (SEC-009) is kept as a cleanly bounded SPI, while the
  operational default in deployment is the verified asymmetric signer.

## Owned Paths

- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/config/AuthSessionConfiguration.kt`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/provider/`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/login/LoginVerificationService.kt`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/session/TokenSessionService.kt`
- `app/accounts/src/main/kotlin/com/squarewise/accounts/auth/jwks/`
- `app/*/src/main/kotlin/**/security/`
- `contracts/rest/accounts.openapi.json`
- `docs/tasks/details/SEC-01A.md`

## Acceptance Criteria

1. Accounts configures a production-ready asymmetric JWT token provider backed by RSA
   key material configured via properties/environment (with secure fallback for local dev).
2. The JWKS endpoint is exposed publicly and returns RFC 7517 compliant JWKS JSON
   containing the active public key and any retiring rotation keys.
3. Passwordless verification generates a valid access token accepted by all resource
   servers (`accounts`, `expense-core`, `notifications`, `bff`) with matching `iss` and `aud`.
4. `ExternalOidcTokenProvider` throwing stub is replaced or re-wired so deployed profiles
   never throw `UnsupportedOperationException` on passwordless token issuance.
5. All negative token tests pass: wrong signature, expired token, wrong issuer,
   wrong audience, unknown `kid`, and invalid algorithm fail closed with RFC 9457 errors.
6. Local integration and E2E tests demonstrate end-to-end passwordless login in
   production/local-oidc profiles without test mocks.

## Verification Commands

- `./gradlew :libs:security:test --rerun-tasks --no-daemon`
- `./gradlew :app:accounts:test --rerun-tasks --no-daemon`
- `./gradlew :app:bff:test --rerun-tasks --no-daemon`
- `./gradlew :app:expense-core:test --rerun-tasks --no-daemon`
- `./gradlew :app:notifications:test --rerun-tasks --no-daemon`
- `uv run python tools/contracts/validate.py`
- `uv run python tools/ops/check_security_hygiene.py`
- `uv run python tools/ops/check_architecture.py`
