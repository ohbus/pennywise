# CORE-03: Implement repayments and reversals

- Phase: future_implementation
- Owner role: core
- Dependencies: CORE-02
- Owned paths: `app/expense-core/`

## Outcome

Implement recorded repayments, suggestions and audited reversals.

## Implementation requirements

Positive participant-reported transfers update balances immediately; either involved active actor can reverse with a reason; suggestions are per currency/group.

## Acceptance criteria

Overpayment is represented; reversal is idempotent; no claim of verified external money movement or globally minimal transfer count.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

## Current increment

Added an exact minor-unit settlement service supporting positive participant
transfers and idempotent audited reversal status. REST exposure, authorization,
balance projection, persistence, and concurrent integration tests remain pending.

The REST boundary now exposes validated settlement recording and reversal paths
over the domain service. Group authorization, persistent group association, and
full record/reverse HTTP integration coverage remain pending.
