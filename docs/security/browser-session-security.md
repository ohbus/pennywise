# Browser session security

Browser cookie delivery is not enabled by AUTH-08. The current REST and GraphQL
boundaries accept bearer tokens and therefore do not issue JavaScript-readable or
HttpOnly refresh-token cookies.

When AUTH-12 implements browser sessions, it must use a BFF-owned `Secure`,
`HttpOnly`, `SameSite` cookie, reject cross-site state-changing requests with an
explicit origin/CSRF policy, and clear the cookie on logout, expiry, and account
deletion. Refresh tokens must never appear in URLs, browser storage, logs, or
GraphQL variables exposed to browser code.

This is a planned boundary, not current runtime evidence.
