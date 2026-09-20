# AUTH-05 — Remove weaker local authentication modes

## Objective

Ensure local application execution uses the same signed OIDC JWT validation
path as staging and production. There is no local passthrough identity mode.
Missing issuer, audience, discovery, or signing keys must prevent protected
application startup or reject requests; local convenience must not weaken
security.

## Decision

`local-oidc` is the only supported application security profile for local
containers and native runs. Keycloak is the local issuer adapter, while
Pennywise login/session and authorization behavior remain the same as every
other environment. The former `local` opaque-token passthrough is removed,
not merely hidden behind a weaker opt-in profile.

## Acceptance criteria

- No application runtime class accepts a raw bearer string as a subject.
- Local Compose and documented native runs select `local-oidc`.
- Missing local OIDC settings fail closed.
- Local, staging, and production use the same decoder, claim policy, audience,
  issuer, algorithm, expiry, and subject rules.
- Focused compilation/security tests, Compose validation, and documentation
  checks pass.

## Status

In progress. Runtime passthrough classes have been removed and local live
acceptance/load entry points now require signed bearer injection; final closure
remains tracked by AUTH-06 for seeded-user fixture replacement and by AUTH-07
for the complete passwordless journey evidence.
