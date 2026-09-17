# CORE-02: Implement expense allocation and ledger

- Phase: future_implementation
- Owner role: core
- Dependencies: CORE-01
- Owned paths: `app/expense-core/`

## Outcome

Implement expense CRUD, allocation, postings, balances and audit.

## Implementation requirements

Support positive expenses, multiple payers, equal/exact/basis-point/weight allocations, deterministic rounding and @Version.

## Acceptance criteria

Expense/postings/balance/audit/sync/outbox commit atomically; stale edits conflict; duplicate create cannot resurrect deleted records; property tests preserve zero sum.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

