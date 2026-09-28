# Authentication lifecycle threat model

## Assets

- Access tokens.
- Refresh tokens.
- Session-family state.
- Provider-qualified identities.
- Login credentials.
- Browser session cookies.
- Account and membership authorization.
- Financial operations.

## Threats and required controls

| Threat | Required control |
|---|---|
| Stolen access token | Short lifetime, audience restriction, TLS, optional DPoP |
| Stolen refresh token | Rotation, sender constraint or replay detection, secure storage |
| Refresh replay | Family revocation and generic unauthorized response |
| Concurrent refresh | Atomic compare-and-set and deterministic failure policy |
| Idle abuse | Refresh-token inactivity expiry |
| Infinite sliding session | Immutable absolute expiry |
| Logout bypass | Writer-authoritative family revocation |
| Caller-selected identity | Resolve subject/account from trusted server state |
| Email enumeration | Generic login responses and bounded errors |
| Browser CSRF | SameSite cookies, CSRF tokens, origin checks |
| Browser token theft | HttpOnly cookies and BFF server-side token handling |
| Token leakage | Redaction, no URL tokens, no sensitive telemetry |
| Provider outage | Fail closed for issuance and validation uncertainty |
| Cache staleness | Cache never overrides revocation or authorization |
| Cross-group access | Service-owned membership authorization |
| WebSocket bypass | Handshake and reconnect validation |

## Evidence rule

Each threat requires a named test or operational evidence item. A design
statement without executable evidence is not sufficient for release readiness.
