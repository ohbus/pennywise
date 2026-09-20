# AUTH-06 — Keycloak environment and meaningful Compose hostnames

## Objective

Provide a real Keycloak OIDC environment for local, staging, and production-
like validation while keeping the application code provider-neutral. All
Compose services must have stable, meaningful hostnames and internal URLs must
use those names rather than localhost or ambiguous container identifiers.

## Requirements

- Keycloak is the current provider in every environment.
- Realm/client/bootstrap configuration is externalized and contains no
  production secrets.
- Mailpit is the local SMTP sink for passwordless-link/code tests.
- The local Keycloak image is pinned centrally in `infra/versions.env.example`
  and imports `infra/local/keycloak/realm/pennywise-realm.json`.
- Keycloak issuer, JWKS, and application audience are configured through
  environment variables.
- Compose hostnames use `idp-keycloak`, `accounts-api`, `expense-core-api`,
  `notifications-api`, `pennywise-bff`, `postgres-db`, `message-broker`, and
  `mailpit-email`.
- Health checks and startup dependencies use these stable names.
- Real signed tokens replace arbitrary local bearer strings in OIDC E2E tests.

The Compose/Keycloak integration is an infrastructure adapter inside the
authentication boundary. It must not leak realm/bootstrap details into domain
or application packages and must remain replaceable through the OIDC provider
port.

## Acceptance criteria

- Dependency and full-stack Compose configurations render successfully.
- Keycloak starts healthy and exposes discovery/JWKS through its meaningful
  hostname.
- A seeded local user can obtain a signed token for the Pennywise audience.
- All four applications accept valid tokens and reject invalid/expired tokens.
- Mailpit receives passwordless authentication messages without leaking codes
  into application logs.
- Auth0/another OIDC provider can later replace the issuer by configuration.

## Current evidence

- Official Keycloak container guidance was checked before pinning the image;
  realm import uses `/opt/keycloak/data/import` and `--import-realm`.
- Keycloak `26.7.4` started from the full local Compose topology.
- The `pennywise` realm imported successfully from the checked-in fixture.
- OIDC discovery returned HTTP 200 at
  `http://localhost:8090/realms/pennywise/.well-known/openid-configuration`.
- Application token acquisition and real-token REST/GraphQL/WebSocket journeys
  remain open; the current applications still run under the local passthrough
  profile.
- Operations, README, Bruno environment, and E2E instructions are updated.
