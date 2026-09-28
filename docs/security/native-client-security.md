# Native client security

Native clients are not granted a special passwordless bearer shortcut by
AUTH-08. The intended deployment flow is OAuth 2.0 Authorization Code with PKCE
through the system browser, using an external user agent and platform secure
storage for refresh tokens, consistent with RFC 8252 and RFC 7636.

The current Accounts API retains an explicit `clientKind` value for session
metadata, but refresh rotation restores that value from the persisted session;
the refresh caller cannot change it. Native authorization-code and secure-storage
integration remain planned work and are not claimed as implemented here.
