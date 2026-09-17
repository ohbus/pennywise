# OPS-03: Modular container and Compose workflows

- Phase: operations
- Owner: coordinator
- Dependencies: FND-01, FND-02
- Owned paths: `infra/docker/`, `infra/local/docker-compose.dev.yml`, `infra/deploy/`

## Outcome

Provide one development Dockerfile, one production JVM Dockerfile with an app
argument, local dependency/app Compose configuration, and production immutable
image configuration with example variables.

## Acceptance

Dockerfiles are non-root where applicable, production config contains no secret,
Compose files pass config validation, and the development quickstart explains
which endpoints are not yet implemented.
