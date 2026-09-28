# Squarewise passwordless API contract

This contract is provider-neutral. It is the same in local, staging, and
production; only the configured OIDC/email adapters and secrets differ.

## `POST /accounts/v1/auth/login/start`

Request:

```json
{"email":"person@example.com","channel":"LINK"}
```

The email is canonicalized by `EmailAddress`. The endpoint returns `202 Accepted`
with the same response shape when the request is admitted, whether the account
exists:

```json
{"status":"accepted","retryAfterSeconds":60}
```

When the per-email or per-network abuse policy is exhausted, the endpoint
returns the structured application limit response `429 Too Many Requests` with
code `RATE_LIMITED` and a bounded `Retry-After: 60` header. The service queues a
short-lived, single-use link or code only when policy allows it. Neither outcome
reports account existence or whether delivery succeeded.

## `POST /accounts/v1/auth/login/verify`

Request:

```json
{"credential":"one-time-value","mode":"CODE"}
```

The credential is compared against a hash, atomically marked consumed, and
rejected when expired, replayed, over-attempted, or revoked. Successful
verification creates the configured Squarewise session/token response. Raw
credentials never appear in logs, URLs, traces, metrics, or errors. Successful
token responses include `Cache-Control: no-store` and `Pragma: no-cache`; clients
and intermediaries must not persist access or refresh token response bodies.

## `POST /accounts/v1/auth/token/refresh`

Request:

```json
{"refreshToken":"opaque-refresh-token"}
```

Refresh tokens are opaque, hashed at rest, rotated on every successful use,
short-lived relative to the account session, and bound to a token family. A
network-partition refresh limit is checked before rotation; exhaustion returns
`429 RATE_LIMITED` with `Retry-After: 60`. Any reuse of an old token remains a
generic `401` response and revokes the complete family. Successful refresh
responses include the same `no-store` and `no-cache` directives.

## `POST /accounts/v1/auth/logout`

Revokes the current session/token family and returns `204 No Content`. Repeated
logout is idempotent and does not disclose session existence.

## Security requirements

The browser-facing BFF proxies these operations without exposing the Accounts
token body. Verification sets Secure, HttpOnly access/refresh cookies; refresh
and logout require the exact configured Origin plus a matching readable CSRF
nonce/header pair. Native clients continue to call the Accounts contract with
bearer-token storage governed by the native-client policy.

- Generic anti-enumeration responses and bounded request cost.
- Email normalization before lookup, throttling, or credential issuance.
- Atomic single-use redemption and concurrent redemption tests.
- Per-email and per-network rate limits with resend cooldown and attempt caps.
- Browser sessions use Secure, HttpOnly, SameSite cookies and CSRF protection.
- Native clients use short-lived access tokens and rotating refresh tokens.
- OAuth authorization-code integrations use PKCE `S256`, exact redirects,
  transaction-bound state/nonce, issuer mix-up protection, and no implicit or
  password grants, consistent with RFC 9700.
- Downstream resource authorization remains independent of authentication.
