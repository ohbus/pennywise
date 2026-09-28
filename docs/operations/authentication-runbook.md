# Authentication runbook

## Normal checks

1. Confirm the service readiness endpoint and OIDC discovery endpoint are healthy.
2. Confirm issuer, audience, and allowed signing algorithms are present in the
   deployment configuration.
3. Confirm Redis is available for distributed login and refresh rate limiting.
4. Confirm Accounts writer connectivity before testing refresh or logout.
5. Use a newly minted signed test token; never reuse a production credential in
   diagnostics.

Ordinary bearer requests validate locally and must not require an Accounts or
`auth_sessions` lookup. Refresh and logout are writer-authoritative operations.

## Evidence boundary

Local Gradle and Compose checks do not prove hosted OIDC, production capacity,
HA/DR, key rotation, restore, or incident-response readiness. Record those as
separate evidence in the progress ledger.
