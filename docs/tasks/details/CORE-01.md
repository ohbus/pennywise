# CORE-01: Implement groups membership invitations

- Phase: future_implementation
- Owner role: core
- Dependencies: FND-02
- Owned paths: `app/expense-core/`

## Outcome

Implement groups, immutable participant IDs, roles, invites and claiming.

## Implementation requirements

Use expiring/revocable single-use claim invitations, immutable historical participant references and group revision locking.

## Acceptance criteria

Concurrent claims cannot double-link; removal prevents future access and new-expense inclusion while retaining history.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added validated group creation and member-scoped listing at `/expense-core/v1/groups`
with immutable generated group IDs and revision `0`. This is a contract slice;
membership invitations, persistence, revision locking, and concurrent single-use
claims remain pending. The invitation endpoints are now added with bounded
expiry, token invalidation on claim, and member-scoped claim behavior; durable
storage and database locking remain pending.
