# BFF-02: Implement live update fanout

- Phase: future_implementation
- Owner role: bff
- Dependencies: BFF-01, MSG-01, CORE-04
- Owned paths: `app/bff/`

## Outcome

Implement per-replica realtime invalidation and recovery.

## Current increment

Added a database-free, thread-safe per-replica fanout abstraction. Subscriptions
are scoped to a user and group, updates contain only group ID and revision, and
each subscriber has a bounded FIFO queue. A full queue drops the new update and
returns no delivery count; clients must recover through the revision sync flow.
Unsubscribe removes all future delivery. This slice deliberately has no money
state or authorization responsibility; those remain at the subscription edge
and Expense Core synchronization boundary.

## Implementation requirements

Each BFF instance has a temporary fanout queue; minimal IDs/revisions; authenticate and authorize subscriptions; handle expiry/revocation.

## Acceptance criteria

Two replicas notify their own clients; broker reconnect triggers resync; slow consumers are bounded; money never depends on socket delivery.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.
