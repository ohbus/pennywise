# Browser session security

Browser cookie delivery is not enabled by AUTH-08. The current REST and GraphQL
boundaries accept bearer tokens and therefore do not issue JavaScript-readable or
HttpOnly refresh-token cookies. The BFF GraphQL transport now enforces an exact
allow-list through `SQUAREWISE_SECURITY_BROWSER_ALLOWED_ORIGINS`; missing origins
remain accepted for native/non-browser clients, while a supplied origin that is
not an exact scheme/host/port match receives `403 Forbidden`. Preflight requests
are answered only for an allow-listed origin and never grant credentials or a
wildcard origin.

When AUTH-12 implements browser sessions, it must use a BFF-owned `Secure`,
`HttpOnly`, `SameSite` cookie, reject cross-site state-changing requests with an
explicit origin/CSRF policy, and clear the cookie on logout, expiry, and account
deletion. Refresh tokens must never appear in URLs, browser storage, logs, or
GraphQL variables exposed to browser code.

Cookie delivery and CSRF protection remain a planned AUTH-12 boundary, not
current runtime behavior. The exact-origin BFF transport rule is covered by the
focused GraphQL transport tests; deployment must provide explicit origins before
browser clients are enabled.
