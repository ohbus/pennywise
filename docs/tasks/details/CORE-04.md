# CORE-04: Implement offline synchronization

- Phase: future_implementation
- Owner role: core
- Dependencies: CORE-02, CORE-03
- Owned paths: `app/expense-core/`

## Outcome

Implement snapshot/change-feed and offline-create recovery.

## Implemented increment

`SynchronizationStore` provides transactionally serialized revisions, deterministic
ascending change pages, tombstones for deletions, opaque URL-safe cursors, and
clock-based cursor expiry. A stale or malformed cursor fails closed with
`InvalidSyncCursorException`, allowing the caller to request a fresh snapshot
without losing locally queued creates. The in-memory store is a contract/domain
slice; persistence, authorization replay checks, and transport wiring remain.

The Expense Core REST controller exposes authenticated snapshot and changes routes
with bounded `limit`, opaque cursor forwarding, tombstone responses, and stable
page metadata. Invalid cursors and limits fail with HTTP 400.

Verification: `./gradlew :app:expense-core:test` passed. Tests cover ordering,
pagination, deletion propagation, cursor round trips, expiry, invalid cursors,
and limit bounds.

## Implementation requirements

Use transactionally serialized group revisions and paginated materialized snapshots; cursor expiry forces fresh snapshot without dropping queued creates.

## Acceptance criteria

Concurrency cannot skip committed changes; deletions propagate; authorization loss rejects replay; tests simulate lost response and retry after edits/deletion.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.
