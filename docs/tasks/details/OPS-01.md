# OPS-01: Implement deployment telemetry and release

- Phase: future_implementation
- Owner role: platform
- Dependencies: FND-02, FND-03
- Owned paths: `infra/deploy/`, `docs/operations/`

## Outcome

Create production reference deployment, telemetry and release procedures.

## Implementation requirements

Use containers/private networking, external TLS, scoped secrets, migration-before-rollout, readiness/shutdown, JSON logs and OTLP.

## Acceptance criteria

Rehearse backward-compatible migrations and rollback; do not claim cloud resource provisioning without chosen provider.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Implementation notes

The production Compose reference uses immutable image variables, a private
internal network, read-only containers, a bounded writable `/tmp`, privilege
escalation protection, init-based signal forwarding, Actuator readiness checks,
structured logs, and graceful shutdown. It does not provision a database,
broker, TLS terminator, or cloud resources; those are external dependencies.

Run expand migrations before changing application images, retain the previous
image digest during rollout, and test rollback against the resulting schema.
