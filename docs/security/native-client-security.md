# Native client security

Native clients are not granted a special passwordless bearer shortcut by
AUTH-08. The intended deployment flow is OAuth 2.0 Authorization Code with PKCE
through the system browser, using an external user agent and platform secure
storage for refresh tokens, consistent with RFC 8252 and RFC 7636.

The current Accounts API retains an explicit `clientKind` value for session
metadata, but refresh rotation restores that value from the persisted session;
the refresh caller cannot change it. The BFF cookie boundary is for browsers;
native clients continue to use the Accounts bearer-token contract and must not
copy browser cookies. The local OIDC realm enables authorization-code flow with
PKCE S256 while direct access grants remain disabled. Full native application
integration and platform secure-storage evidence remain client/UI work, but the
protocol policy is enforced by provider configuration.
