# DOC-04: Specify finance and REST APIs

- Phase: documentation
- Owner role: contracts
- Dependencies: DOC-01
- Owned paths: `contracts/rest/`, `contracts/examples/`

## Outcome

Author OpenAPI 3.1 REST documents, financial invariants and executable examples.

## Implementation requirements

Cover Accounts, Expense Core and Notifications including authorization, error shapes, idempotency and optimistic concurrency.

## Acceptance criteria

Allocation examples preserve exact totals; service operations cover every MVP capability; no financial floating-point wire values.

## Verification and handoff

Record exact commands, exit status, artifact paths, findings and unresolved blockers
in the task registry. No task is done until its deliverable is reviewed. Use the
approved contracts as the behavioral authority; update the specification and
register child tasks before expanding scope. Future implementation tasks are not
authorized to bypass the documentation gate or the current scaffold milestone.

