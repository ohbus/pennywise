# OPS-02: Verify recovery capacity cost and launch

- Phase: future_implementation
- Owner role: quality
- Dependencies: QA-01, OPS-01
- Owned paths: `tests/performance/`, `docs/operations/`

## Outcome

Measure release readiness, recovery, load and cost.

## Implementation requirements

Run restore/reconciliation drills, representative load and reconnect storms; report infrastructure footprint and monthly cost assumptions.

## Acceptance criteria

Capacity and recovery objectives are backed by evidence; launch market/retention/hosting/budget gates resolved before public launch.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Increment evidence (recovery and capacity probes)

Added `tests/performance/capacity-smoke.sh`, `recovery-drill.sh`, and
`cost-estimate.sh`, with usage and limitations in `tests/performance/README.md`.
These are environment-driven, reproducible probes. They do not represent cloud
capacity, backup restoration, or provider pricing until run against an isolated
environment and attached to release evidence.
