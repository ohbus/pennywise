# DOC-05: Specify events and offline synchronization

- Phase: documentation
- Owner role: contracts
- Dependencies: DOC-04
- Owned paths: `contracts/events/`, `contracts/examples/`

## Outcome

Author event JSON schemas and offline/recurrent processing semantics.

## Implementation requirements

Document group commit revisions, materialized snapshots, tombstones, queued creation, membership revocation, inbox/outbox deduplication, recurrence occurrence identity.

## Acceptance criteria

Broker loss/reorder and expired cursor recovery have explicit outcomes; external email is not claimed exactly-once.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

