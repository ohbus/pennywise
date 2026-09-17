# CORE-06: Implement search and export

- Phase: future_implementation
- Owner role: core
- Dependencies: CORE-02
- Owned paths: `app/expense-core/`

## Outcome

Implement authorized search/filtering and CSV export.

## Implementation requirements

Use indexed PostgreSQL queries and bounded pages; separate per-currency totals; escape spreadsheet formula-like text in CSV.

## Acceptance criteria

Cross-group access fails; pagination is stable; export resource use is bounded and financial values round-trip.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added deterministic case-insensitive bounded expense filtering and CSV formula
injection escaping. Authorized PostgreSQL queries, stable cursor pagination,
currency totals, and streaming export remain pending.
