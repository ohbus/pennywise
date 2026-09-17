# CORE-05: Implement recurring expenses

- Phase: future_implementation
- Owner role: core
- Dependencies: CORE-02
- Owned paths: `app/expense-core/`

## Outcome

Implement database-backed recurring expense scheduling.

## Implementation requirements

Generate frozen expense occurrences using stable schedule/occurrence IDs; weekly/monthly local dates, timezones and month-end clamp.

## Acceptance criteria

Worker restart cannot duplicate occurrences; schedule edits affect future only; invalid membership pauses and notifies.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added deterministic weekly/monthly recurrence calculation with local-date month
end clamping. Database schedules, frozen occurrence IDs, timezone execution,
worker idempotency, and membership pause notifications remain pending.

Added deterministic occurrence IDs derived from schedule ID and local occurrence
date so retries address one frozen occurrence. Durable uniqueness constraints and
worker claiming remain pending.
