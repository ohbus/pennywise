# Pennywise passwordless API contract

This contract is provider-neutral. It is the same in local, staging, and
production; only the configured OIDC/email adapters and secrets differ.

## `POST /accounts/v1/auth/login/start`

Request:

```json
{"email":"person@example.com","channel":"LINK"}
```

The email is canonicalized by `EmailAddress`. The endpoint always returns
`202 Accepted` with the same response shape whether the account exists:

```json
{"status":"accepted","retryAfterSeconds":60}
```

The service queues a short-lived, single-use link or code only when policy
allows it. It never reports account existence, whether delivery succeeded, or
whether a request was throttled beyond the generic retry response.

## `POST /accounts/v1/auth/login/verify`

Request:

```json
{"credential":"one-time-value","mode":"CODE"}
```

The credential is compared against a hash, atomically marked consumed, and
rejected when expired, replayed, over-attempted, or revoked. Successful
verification creates the configured Pennywise session/token response. Raw
credentials never appear in logs, URLs, traces, metrics, or errors.

## `POST /accounts/v1/auth/token/refresh`

Request:

```json
{"refreshToken":"opaque-refresh-token"}
```

Refresh tokens are opaque, hashed at rest, rotated on every successful use,
short-lived relative to the account session, and bound to a token family. Any
reuse of an old token revokes the complete family and returns `401`.

## `POST /accounts/v1/auth/logout`

Revokes the current session/token family and returns `204 No Content`. Repeated
logout is idempotent and does not disclose session existence.

## Security requirements

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
