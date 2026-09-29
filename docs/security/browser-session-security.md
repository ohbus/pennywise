# Browser session security

The BFF owns the browser session boundary. `POST /auth/login/start` and
`POST /auth/login/verify` proxy the Squarewise-owned passwordless flow; successful
verification sets Secure, HttpOnly, SameSite=Lax access and rotating refresh
cookies. Browser responses contain only session metadata, so JavaScript never
receives either credential. The access cookie is adapted to the existing GraphQL
bearer boundary server-side.

`POST /auth/token/refresh` and `POST /auth/logout` require both an exact
allow-listed `Origin` and a double-submit CSRF proof: the readable nonce cookie
must equal `X-CSRF-Token`. The refresh cookie is scoped to `/auth`; the access
cookie is scoped to `/`; logout clears all three cookies. Refresh tokens never
appear in URLs, browser storage, logs, or GraphQL variables exposed to browser
code.

`SQUAREWISE_SECURITY_BROWSER_ALLOWED_ORIGINS` is an explicit comma-separated
origin allow-list. Wildcards, paths, credentials in origins, and non-HTTP(S)
schemes are rejected. Credentialed CORS is limited to the browser-auth routes;
origin-less GraphQL requests remain compatible with native bearer clients.
Deployments must set the allow-list before enabling browser clients and serve the
BFF over HTTPS because the cookies are always marked Secure.
