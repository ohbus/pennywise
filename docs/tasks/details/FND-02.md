# FND-02: Configure local infrastructure and migration foundations

- Phase: scaffold
- Owner role: platform
- Dependencies: FND-01
- Owned paths: `infra/`, `libs/`

## Outcome

Add local dependencies and technical persistence/security foundations.

## Implementation requirements

Compose runs PostgreSQL with service-private credentials, RabbitMQ, development OIDC and SMTP capture; JPA validates, Flyway migrates.

## Acceptance criteria

Use environment/secret examples without real credentials; service startup is reproducible; technical infrastructure does not implement product features.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

