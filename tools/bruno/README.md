# Bruno API collection

The repository runner is pinned to `@usebruno/cli@4.1.0` (Bruno CLI v4) for
consistent local and CI behavior.

Import `tools/bruno/` into Bruno and select an environment. The collection
covers the Accounts, Expense Core, Notifications, and GraphQL BFF HTTP
surfaces currently defined by the repository contracts, including recurring
schedules and explicit quality/error probes.

The legacy local environment uses `test-user` only for non-OIDC compatibility.
For the Keycloak-backed stack, select `local-oidc` and provide a real signed
access token through `PENNYWISE_BRUNO_TOKEN`; request files never contain a
credential. The complete
stack must be running first:

```sh
make full-up
make full-status
```

Authentication is centralized in the environment `token` variable; request
files never contain environment-specific credentials. Override it without
editing the collection:

```sh
make bruno-run BRUNO_ENV=local-oidc BRUNO_TOKEN="${PENNYWISE_BRUNO_TOKEN}"
make bruno-run BRUNO_ENV=local-oidc BRUNO_TOKEN="${PENNYWISE_BRUNO_TOKEN}"
```

Base URLs are likewise environment variables (`baseUrlBff`,
`baseUrlAccounts`, `baseUrlExpenseCore`, and `baseUrlNotifications`). Requests
that create groups, expenses, memberships, or invites save response identifiers
into scoped Bruno variables for subsequent requests. Dynamic UUIDs make repeat
runs safe; the ordered lifecycle folder archives generated groups last.

`staging-template.bru` is a non-routable template: replace URLs through a
private environment or `--env-var` values and inject the token from a secret
store. Never commit a real staging or production token.

The collection is intentionally local-only: it contains no production hosts,
secrets, or environment files.
